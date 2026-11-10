package com.comeoften.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.store.constant.StoreBusinessStatus;
import com.comeoften.module.store.converter.StoreConverter;
import com.comeoften.module.store.dto.BusinessHourDTO;
import com.comeoften.module.store.dto.StoreTableDTO;
import com.comeoften.module.store.dto.StoreUpdateDTO;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.entity.StoreBusinessHour;
import com.comeoften.module.store.entity.StoreTable;
import com.comeoften.module.store.mapper.StoreBusinessHourMapper;
import com.comeoften.module.store.mapper.StoreMapper;
import com.comeoften.module.store.mapper.StoreTableMapper;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.module.store.vo.StoreVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 门店、营业时间与桌台。营业时间按星期 upsert，避免逻辑删除记录与 uk_store_week 唯一索引冲突。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StoreMapper storeMapper;

    private final StoreBusinessHourMapper businessHourMapper;

    private final StoreTableMapper storeTableMapper;

    @Override
    public Long defaultStoreId() {
        Store store = storeMapper.selectOne(new LambdaQueryWrapper<Store>()
                .orderByAsc(Store::getId).last("limit 1"));
        if (store == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        return store.getId();
    }

    @Override
    public void requireOpen(Long storeId) {
        Store store = require(storeId);
        if (!StoreBusinessStatus.isOpen(store.getBusinessStatus())) {
            throw new BizException(ErrorCode.STORE_CLOSED);
        }
    }

    @Override
    public List<StoreVO> list() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>()
                .orderByAsc(Store::getId));
        if (stores.isEmpty()) {
            return Collections.emptyList();
        }
        return stores.stream().map(store -> detail(store.getId())).collect(Collectors.toList());
    }

    @Override
    public StoreVO detail(Long storeId) {
        Store store = require(storeId);
        List<StoreBusinessHour> hours = businessHourMapper.selectList(new LambdaQueryWrapper<StoreBusinessHour>()
                .eq(StoreBusinessHour::getStoreId, storeId)
                .orderByAsc(StoreBusinessHour::getWeekDay));
        List<StoreTable> tables = storeTableMapper.selectList(new LambdaQueryWrapper<StoreTable>()
                .eq(StoreTable::getStoreId, storeId)
                .orderByAsc(StoreTable::getTableNo));
        return StoreConverter.toVO(store, hours, tables);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long storeId, StoreUpdateDTO dto) {
        require(storeId);
        Store patch = new Store();
        patch.setId(storeId);
        patch.setStoreName(dto.getStoreName());
        patch.setAddress(emptyIfNull(dto.getAddress()));
        patch.setPhone(emptyIfNull(dto.getPhone()));
        patch.setLongitude(dto.getLongitude());
        patch.setLatitude(dto.getLatitude());
        patch.setBusinessStatus(dto.getBusinessStatus() == null ? 1 : dto.getBusinessStatus());
        storeMapper.updateById(patch);
        if (dto.getBusinessHours() != null) {
            upsertBusinessHours(storeId, dto.getBusinessHours());
        }
        log.info("门店信息已更新: storeId={}", storeId);
    }

    @Override
    public Long saveTable(Long storeId, StoreTableDTO dto) {
        require(storeId);
        if (dto.getId() != null) {
            StoreTable existing = storeTableMapper.selectById(dto.getId());
            if (existing == null || !Objects.equals(existing.getStoreId(), storeId)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "桌台不存在");
            }
        }
        List<StoreTable> sameNo = storeTableMapper.selectList(new LambdaQueryWrapper<StoreTable>()
                .eq(StoreTable::getStoreId, storeId)
                .eq(StoreTable::getTableNo, dto.getTableNo()));
        if (sameNo.stream().anyMatch(item -> !item.getId().equals(dto.getId()))) {
            throw new BizException(ErrorCode.PARAM_INVALID, "同门店下桌号已存在");
        }
        StoreTable table = new StoreTable();
        table.setId(dto.getId());
        table.setStoreId(storeId);
        table.setTableNo(dto.getTableNo());
        table.setTableType(dto.getTableType());
        table.setCapacity(dto.getCapacity());
        table.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        if (dto.getId() == null) {
            int restored = storeTableMapper.restoreByNo(storeId, dto.getTableNo(),
                    dto.getTableType(), dto.getCapacity(), table.getStatus());
            if (restored > 0) {
                StoreTable revived = storeTableMapper.selectOne(new LambdaQueryWrapper<StoreTable>()
                        .eq(StoreTable::getStoreId, storeId)
                        .eq(StoreTable::getTableNo, dto.getTableNo()));
                return revived.getId();
            }
            storeTableMapper.insert(table);
        } else {
            storeTableMapper.updateById(table);
        }
        return table.getId();
    }

    @Override
    public StoreVO.StoreTableVO bindTable(Long storeId, String tableCode) {
        if (!StringUtils.hasText(tableCode)) {
            throw new BizException(ErrorCode.STORE_TABLE_BIND_INVALID);
        }
        StoreTable table = storeTableMapper.selectOne(new LambdaQueryWrapper<StoreTable>()
                .eq(StoreTable::getStoreId, storeId)
                .eq(StoreTable::getTableNo, tableCode.trim())
                .last("limit 1"));
        if (table == null) {
            throw new BizException(ErrorCode.STORE_TABLE_NOT_FOUND);
        }
        return StoreConverter.toTableVO(table);
    }

    @Override
    public void updateTableStatus(Long storeId, Long tableId, Integer status) {
        require(storeId);
        StoreTable table = storeTableMapper.selectById(tableId);
        if (table == null || !Objects.equals(table.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "桌台不存在");
        }
        StoreTable patch = new StoreTable();
        patch.setId(tableId);
        patch.setStatus(status);
        storeTableMapper.updateById(patch);
        log.info("桌台状态已更新: storeId={}, tableId={}, status={}", storeId, tableId, status);
    }

    @Override
    public void removeTable(Long storeId, Long tableId) {
        require(storeId);
        StoreTable table = storeTableMapper.selectById(tableId);
        if (table == null || !Objects.equals(table.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "桌台不存在");
        }
        storeTableMapper.deleteById(tableId);
    }

    private void upsertBusinessHours(Long storeId, List<BusinessHourDTO> hours) {
        Set<Integer> seen = new HashSet<>();
        List<StoreBusinessHour> existing = businessHourMapper.selectList(new LambdaQueryWrapper<StoreBusinessHour>()
                .eq(StoreBusinessHour::getStoreId, storeId));
        java.util.Map<Integer, Long> idByWeekDay = existing.stream()
                .collect(Collectors.toMap(StoreBusinessHour::getWeekDay,
                        StoreBusinessHour::getId, (left, right) -> left));
        for (BusinessHourDTO hour : hours) {
            if (!seen.add(hour.getWeekDay())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "同一星期不能重复提交");
            }
            LocalTime open = LocalTime.parse(hour.getOpenTime(), TIME_FORMATTER);
            LocalTime close = LocalTime.parse(hour.getCloseTime(), TIME_FORMATTER);
            StoreBusinessHour record = new StoreBusinessHour();
            record.setWeekDay(hour.getWeekDay());
            record.setOpenTime(open);
            record.setCloseTime(close);
            Long id = idByWeekDay.get(hour.getWeekDay());
            if (id == null && businessHourMapper.restore(storeId, hour.getWeekDay()) > 0) {
                StoreBusinessHour revived = businessHourMapper.selectOne(
                        new LambdaQueryWrapper<StoreBusinessHour>()
                                .eq(StoreBusinessHour::getStoreId, storeId)
                                .eq(StoreBusinessHour::getWeekDay, hour.getWeekDay()));
                id = revived == null ? null : revived.getId();
            }
            if (id == null) {
                record.setStoreId(storeId);
                businessHourMapper.insert(record);
            } else {
                record.setId(id);
                businessHourMapper.updateById(record);
            }
        }
    }

    private Store require(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        return store;
    }

    private static String emptyIfNull(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
