package com.comeoften.module.store.service;

import com.comeoften.module.store.dto.StoreTableDTO;
import com.comeoften.module.store.dto.StoreUpdateDTO;
import com.comeoften.module.store.vo.StoreVO;

import java.util.List;

/**
 * 门店、营业时间与桌台管理。桌号与容量全部由数据配置。
 */
public interface StoreService {

    /**
     * 当前唯一门店 ID。一期只落 1 条 store 数据，从表里取而不是写死常量。
     */
    Long defaultStoreId();

    List<StoreVO> list();

    StoreVO detail(Long storeId);

    void update(Long storeId, StoreUpdateDTO dto);

    Long saveTable(Long storeId, StoreTableDTO dto);

    /**
     * 只改桌台状态（0-空闲 / 1-占用 / 2-已预约）。商家端桌台看板点格改状态走这里，
     * 不改桌号与容量，配置类操作仍留在 PC 后台。
     */
    void updateTableStatus(Long storeId, Long tableId, Integer status);

    void removeTable(Long storeId, Long tableId);
}
