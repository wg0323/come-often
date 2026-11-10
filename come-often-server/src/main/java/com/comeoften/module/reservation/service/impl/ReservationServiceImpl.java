package com.comeoften.module.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.common.util.DynamicCodeSigner;
import com.comeoften.config.CouponProperties;
import com.comeoften.config.ReservationProperties;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.reservation.constant.ReservationConstants;
import com.comeoften.module.reservation.constant.ReservationRoomType;
import com.comeoften.module.reservation.constant.ReservationStatus;
import com.comeoften.module.reservation.constant.ReservationStatusTransition;
import com.comeoften.module.reservation.constant.ReservationTableType;
import com.comeoften.module.reservation.converter.ReservationConverter;
import com.comeoften.module.reservation.dto.ReservationCheckinDTO;
import com.comeoften.module.reservation.dto.ReservationCreateDTO;
import com.comeoften.module.reservation.dto.ReservationRescheduleDTO;
import com.comeoften.module.reservation.entity.Reservation;
import com.comeoften.module.reservation.entity.ReservationSlot;
import com.comeoften.module.reservation.entity.ReservationStatusLog;
import com.comeoften.module.reservation.mapper.ReservationMapper;
import com.comeoften.module.reservation.mapper.ReservationSlotMapper;
import com.comeoften.module.reservation.mapper.ReservationStatusLogMapper;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.module.reservation.service.ReservationSlotService;
import com.comeoften.module.reservation.util.ReservationNoGenerator;
import com.comeoften.module.reservation.vo.ReservationCalendarDayVO;
import com.comeoften.module.reservation.vo.ReservationCodeVO;
import com.comeoften.module.reservation.vo.ReservationMetaVO;
import com.comeoften.module.reservation.vo.ReservationRoomVO;
import com.comeoften.module.reservation.vo.ReservationSlotVO;
import com.comeoften.module.reservation.vo.ReservationTableBoardVO;
import com.comeoften.module.reservation.vo.ReservationTableTypeOptionVO;
import com.comeoften.module.reservation.vo.ReservationVO;
import com.comeoften.module.reservation.vo.TableBoardItemVO;
import com.comeoften.module.reservation.vo.TableTypeStatVO;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.entity.StoreTable;
import com.comeoften.module.store.mapper.StoreMapper;
import com.comeoften.module.store.mapper.StoreTableMapper;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.mq.message.ReservationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 预约订座实现。容量扣减复用 StockService，签到码复用 DynamicCodeSigner，
 * 延迟消息复用 M3 的 MessageProducer + mq_message，不新增第二套机制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private static final long PAGE_MAX_SIZE = 50L;

    private static final long PROBE_MAX_DELAY_SECONDS = 3600L;

    private static final long MIN_DELAY_MILLIS = 1000L;

    private static final String KIND_TIMEOUT = "timeout";

    private static final String KIND_REMIND = "remind";

    private static final String TAB_ACTIVE = "active";

    /** 只有包间能被「指定」；HALL 仍是普通大厅桌台，不出现在可选包间里。 */
    private static final List<String> BOOKABLE_ROOM_TYPES = Arrays.asList(
            ReservationRoomType.PRIVATE_SMALL.name(), ReservationRoomType.PRIVATE_LARGE.name());

    private static final String TAB_FINISHED = "finished";

    private static final String TAB_CANCELLED = "cancelled";

    private final ReservationMapper reservationMapper;

    private final ReservationSlotMapper reservationSlotMapper;

    private final ReservationStatusLogMapper reservationStatusLogMapper;

    private final ReservationSlotService reservationSlotService;

    private final StoreMapper storeMapper;

    private final StoreTableMapper storeTableMapper;

    private final MemberMapper memberMapper;

    private final ReservationProperties reservationProperties;

    private final CouponProperties couponProperties;

    private final MessageProducer messageProducer;

    @Override
    public ReservationMetaVO meta(Long storeId) {
        Store store = requireStoreEntity(storeId);
        ReservationMetaVO vo = new ReservationMetaVO();
        vo.setStoreId(storeId);
        vo.setStoreName(store.getStoreName());
        vo.setDaysAhead(reservationProperties.getDaysAhead());
        LocalDate today = LocalDate.now();
        List<String> dates = new ArrayList<>();
        for (int offset = 0; offset < reservationProperties.getDaysAhead(); offset++) {
            dates.add(today.plusDays(offset).format(DATE_FORMATTER));
        }
        vo.setDates(dates);
        vo.setTimeSlots(reservationProperties.getTimeSlots());
        Map<String, TableTypeStatVO> stats = statMap(storeId);
        List<ReservationTableTypeOptionVO> options = new ArrayList<>();
        for (String code : reservationProperties.getTableTypes()) {
            ReservationTableType type = ReservationTableType.of(code);
            if (type == null) {
                log.warn("comeoften.reservation.table-types 含未知桌型，已跳过: {}", code);
                continue;
            }
            TableTypeStatVO stat = stats.get(code);
            ReservationTableTypeOptionVO option = new ReservationTableTypeOptionVO();
            option.setCode(code);
            option.setName(type.getDescription());
            option.setMinPeople(type.getMinPeople());
            option.setMaxPeople(type.getMaxPeople());
            option.setTableCount(stat == null || stat.getTableCount() == null ? 0 : stat.getTableCount());
            option.setMaxCapacity(stat == null ? null : stat.getMaxCapacity());
            options.add(option);
        }
        vo.setTableTypes(options);
        vo.setCancelLimitHours(reservationProperties.getCancelLimitHours());
        vo.setNoShowMinutes(reservationProperties.getNoShowMinutes());
        vo.setAutoConfirm(reservationProperties.isAutoConfirm());
        return vo;
    }

    @Override
    public List<ReservationSlotVO> slots(Long storeId, LocalDate reserveDate, boolean includePast) {
        LocalDate date = reserveDate == null ? LocalDate.now() : reserveDate;
        requireBookableDate(date);
        LocalTime nowTime = LocalTime.now();
        boolean today = date.equals(LocalDate.now());
        return reservationSlotService.listByDate(storeId, date).stream()
                .filter(slot -> includePast || !today
                        || LocalTime.parse(slot.getTimeSlot(), TIME_FORMATTER).isAfter(nowTime))
                .map(ReservationConverter::toSlotVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationRoomVO> rooms(Long storeId, LocalDate reserveDate,
                                          String timeSlot, Integer peopleCount) {
        LocalDate date = reserveDate == null ? LocalDate.now() : reserveDate;
        requireBookableDate(date);
        if (!reservationProperties.getTimeSlots().contains(timeSlot)) {
            throw new BizException(ErrorCode.RESERVATION_SLOT_NOT_FOUND);
        }
        List<Long> occupied = reservationMapper.selectOccupiedTableIds(storeId, date, timeSlot);
        Set<Long> occupiedIds = occupied == null ? Collections.emptySet() : new HashSet<>(occupied);
        return storeTableMapper.selectList(new LambdaQueryWrapper<StoreTable>()
                        .eq(StoreTable::getStoreId, storeId)
                        .in(StoreTable::getRoomType, BOOKABLE_ROOM_TYPES)
                        .isNotNull(StoreTable::getCapacity)
                        // 占用中（1）的桌台不参与可选列表，与创建时的校验口径一致
                        .ne(StoreTable::getStatus, 1)
                        .orderByAsc(StoreTable::getTableNo))
                .stream()
                .filter(table -> !occupiedIds.contains(table.getId()))
                .filter(table -> peopleCount == null || table.getCapacity() >= peopleCount)
                .map(ReservationConverter::toRoomVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO create(Long memberId, Long storeId, ReservationCreateDTO dto) {
        validateBookable(dto.getReserveDate(), dto.getTimeSlot(), dto.getTableType(), dto.getPeopleCount());
        Long tableId = resolveRoomTable(storeId, dto);
        LocalDateTime reserveAt = LocalDateTime.of(dto.getReserveDate(),
                LocalTime.parse(dto.getTimeSlot(), TIME_FORMATTER));
        ReservationSlot slot = reservationSlotService.requireSlot(storeId, dto.getReserveDate(),
                dto.getTimeSlot(), dto.getTableType());
        if (!reservationSlotService.occupy(slot)) {
            throw new BizException(ErrorCode.RESERVATION_SLOT_FULL);
        }
        boolean autoConfirm = reservationProperties.isAutoConfirm();
        Reservation entity = new Reservation();
        entity.setStoreId(storeId);
        entity.setMemberId(memberId);
        entity.setReserveDate(dto.getReserveDate());
        entity.setTimeSlot(dto.getTimeSlot());
        entity.setTableType(dto.getTableType());
        entity.setPeopleCount(dto.getPeopleCount());
        // M14 B4：指定包间时创建即锁具体桌台；签到沿用 IFNULL(#{tableId}, table_id) 不会覆盖
        entity.setTableId(tableId);
        entity.setContactName(dto.getContactName().trim());
        entity.setContactPhone(dto.getContactPhone().trim());
        entity.setRemark(dto.getRemark() == null ? "" : dto.getRemark().trim());
        entity.setStatus(autoConfirm ? ReservationStatus.CONFIRMED.name() : ReservationStatus.PENDING.name());
        insertWithRetry(entity);
        // 回读一次拿到 DB 默认值（create_time），创建响应与列表详情保持同一口径
        Reservation persisted = reservationMapper.selectById(entity.getId());
        if (persisted != null) {
            entity = persisted;
        }
        writeLog(entity.getId(), "", entity.getStatus(), memberId,
                autoConfirm ? "会员自助预约，系统自动确认" : "会员自助预约，等待商家确认");
        publishTimeout(entity, reserveAt, timeoutDelayMillis(reserveAt));
        publishRemind(entity, reserveAt, remindDelayMillis(reserveAt));
        log.info("预约已创建: reserveNo={}, memberId={}, slot={} {}, tableType={}, tableId={}",
                entity.getReserveNo(), memberId, dto.getReserveDate(), dto.getTimeSlot(),
                dto.getTableType(), tableId);
        return toVO(entity, memberMapper.selectById(memberId), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO reschedule(Long memberId, String reserveNo, ReservationRescheduleDTO dto) {
        Reservation entity = requireOwned(memberId, reserveNo);
        if (!ReservationStatus.active(entity.getStatus())) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        if (entity.getTableId() != null) {
            // 改期会换日期/时段，创建时锁定的包间不能跟着搬过去；取消后重订才能释放并重新选。
            // 拦截要早于桌型/人数校验，否则改到不匹配的大包间会先返回 30044 而不是可读的 30098。
            throw new BizException(ErrorCode.TABLE_ROOM_TYPE_INVALID, "指定包间的预约请先取消后重新预订");
        }
        validateBookable(dto.getReserveDate(), dto.getTimeSlot(), dto.getTableType(), entity.getPeopleCount());
        if (Objects.equals(entity.getReserveDate(), dto.getReserveDate())
                && Objects.equals(entity.getTimeSlot(), dto.getTimeSlot())
                && Objects.equals(entity.getTableType(), dto.getTableType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "新时段与原时段相同");
        }
        LocalDate oldDate = entity.getReserveDate();
        String oldTimeSlot = entity.getTimeSlot();
        String oldTableType = entity.getTableType();
        ReservationSlot newSlot = reservationSlotService.requireSlot(entity.getStoreId(),
                dto.getReserveDate(), dto.getTimeSlot(), dto.getTableType());
        if (!reservationSlotService.occupy(newSlot)) {
            throw new BizException(ErrorCode.RESERVATION_SLOT_FULL);
        }
        int updated = reservationMapper.reschedule(entity.getId(), dto.getReserveDate(), dto.getTimeSlot(),
                dto.getTableType(), oldDate, oldTimeSlot, oldTableType);
        if (updated == 0) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        ReservationSlot oldSlot = reservationSlotService.requireSlot(entity.getStoreId(), oldDate,
                oldTimeSlot, oldTableType);
        if (!reservationSlotService.releaseInTransaction(oldSlot.getId())) {
            // 旧容量释放失败必须整体回滚，避免两处容量同时被占
            throw new BizException(ErrorCode.SYSTEM_ERROR, "改期释放原时段失败，请稍后重试");
        }
        entity.setReserveDate(dto.getReserveDate());
        entity.setTimeSlot(dto.getTimeSlot());
        entity.setTableType(dto.getTableType());
        writeLog(entity.getId(), entity.getStatus(), entity.getStatus(), memberId,
                "会员改期：" + oldDate + " " + oldTimeSlot + " → " + dto.getReserveDate() + " " + dto.getTimeSlot());
        LocalDateTime reserveAt = LocalDateTime.of(dto.getReserveDate(),
                LocalTime.parse(dto.getTimeSlot(), TIME_FORMATTER));
        publishTimeout(entity, reserveAt, timeoutDelayMillis(reserveAt));
        publishRemind(entity, reserveAt, remindDelayMillis(reserveAt));
        log.info("预约已改期: reserveNo={}, {} {} → {} {}", reserveNo, oldDate, oldTimeSlot,
                dto.getReserveDate(), dto.getTimeSlot());
        return toVO(entity, memberMapper.selectById(memberId), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO cancel(Long memberId, String reserveNo) {
        Reservation entity = requireOwned(memberId, reserveNo);
        if (!ReservationStatus.active(entity.getStatus())) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        LocalDateTime reserveAt = ReservationConverter.reserveAt(entity);
        LocalDateTime now = LocalDateTime.now();
        long cancelLimitHours = reservationProperties.getCancelLimitHours();
        if (reserveAt == null || !now.isBefore(reserveAt.minusHours(cancelLimitHours))) {
            throw new BizException(ErrorCode.RESERVATION_CANCEL_TOO_LATE,
                    "距预约不足 " + cancelLimitHours + " 小时，无法取消");
        }
        String fromStatus = entity.getStatus();
        if (reservationMapper.cancel(entity.getId()) == 0) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        releaseSlot(entity);
        writeLog(entity.getId(), fromStatus, ReservationStatus.CANCELLED.name(), memberId, "会员取消预约");
        entity.setStatus(ReservationStatus.CANCELLED.name());
        log.info("预约已取消: reserveNo={}, memberId={}", reserveNo, memberId);
        return toVO(entity, memberMapper.selectById(memberId), now);
    }

    @Override
    public PageResult<ReservationVO> pageForMember(Long memberId, String tab, Long current, Long size) {
        Page<Reservation> page = new Page<>(Math.max(1L, current == null ? 1L : current), boundSize(size));
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getMemberId, memberId);
        if (TAB_ACTIVE.equals(tab)) {
            wrapper.in(Reservation::getStatus, ReservationStatus.PENDING.name(),
                    ReservationStatus.CONFIRMED.name());
        } else if (TAB_FINISHED.equals(tab)) {
            wrapper.in(Reservation::getStatus, ReservationStatus.CHECKED_IN.name(),
                    ReservationStatus.NO_SHOW.name());
        } else if (TAB_CANCELLED.equals(tab)) {
            wrapper.eq(Reservation::getStatus, ReservationStatus.CANCELLED.name());
        }
        wrapper.orderByDesc(Reservation::getReserveDate).orderByDesc(Reservation::getTimeSlot)
                .orderByDesc(Reservation::getCreateTime);
        IPage<Reservation> result = reservationMapper.selectPage(page, wrapper);
        Member member = memberMapper.selectById(memberId);
        String storeName = storeName(result.getRecords());
        List<ReservationVO> records = result.getRecords().stream()
                .map(entity -> toVO(entity, member, LocalDateTime.now(), storeName))
                .collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public ReservationVO detailForMember(Long memberId, String reserveNo) {
        Reservation entity = requireOwned(memberId, reserveNo);
        ReservationVO vo = toVO(entity, memberMapper.selectById(memberId), LocalDateTime.now());
        vo.setLogs(listLogs(entity.getId()));
        return vo;
    }

    @Override
    public ReservationCodeVO checkinCode(Long memberId, String reserveNo) {
        Reservation entity = requireOwned(memberId, reserveNo);
        if (!ReservationStatus.active(entity.getStatus())) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        long epochSecond = System.currentTimeMillis() / 1000L;
        String secret = requireCodeSecret();
        String code = DynamicCodeSigner.build(ReservationConstants.SIGN_SCOPE, entity.getReserveNo(),
                epochSecond, ReservationConstants.TIME_WINDOW_SECONDS, ReservationConstants.SIGN_LENGTH, secret);
        ReservationCodeVO vo = new ReservationCodeVO();
        vo.setReserveNo(entity.getReserveNo());
        vo.setCode(code);
        vo.setCodeContent(ReservationConstants.CODE_PREFIX + code);
        vo.setSecondsRemaining(DynamicCodeSigner.secondsRemaining(epochSecond,
                ReservationConstants.TIME_WINDOW_SECONDS));
        vo.setReserveAt(ReservationConverter.reserveAt(entity));
        vo.setStatusText(ReservationStatus.describe(entity.getStatus()));
        return vo;
    }

    @Override
    public PageResult<ReservationVO> adminPage(Long storeId, LocalDate reserveDate, String status, String keyword,
                                               Long current, Long size) {
        Page<Reservation> page = new Page<>(Math.max(1L, current == null ? 1L : current), boundSize(size));
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStoreId, storeId)
                .eq(reserveDate != null, Reservation::getReserveDate, reserveDate)
                .eq(StringUtils.hasText(status), Reservation::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            // 前缀模糊，避免全模糊扫描（db-spec 页面搜索限制）
            String like = keyword.trim();
            wrapper.and(inner -> inner.likeRight(Reservation::getReserveNo, like)
                    .or().likeRight(Reservation::getContactName, like)
                    .or().likeRight(Reservation::getContactPhone, like));
        }
        wrapper.orderByDesc(Reservation::getCreateTime);
        IPage<Reservation> result = reservationMapper.selectPage(page, wrapper);
        List<ReservationVO> records = new ArrayList<>();
        Map<Long, Member> members = loadMembers(result.getRecords());
        String storeName = storeName(result.getRecords());
        for (Reservation entity : result.getRecords()) {
            records.add(toVO(entity, members.get(entity.getMemberId()), LocalDateTime.now(), storeName));
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public ReservationVO adminDetail(Long storeId, String reserveNo) {
        Reservation entity = requireStore(storeId, reserveNo);
        ReservationVO vo = toVO(entity, memberMapper.selectById(entity.getMemberId()), LocalDateTime.now());
        vo.setLogs(listLogs(entity.getId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO confirm(Long storeId, Long operatorId, String reserveNo) {
        Reservation entity = requireStore(storeId, reserveNo);
        ReservationStatusTransition.require(entity.getStatus(), ReservationStatus.CONFIRMED.name());
        if (reservationMapper.confirm(entity.getId()) == 0) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        writeLog(entity.getId(), entity.getStatus(), ReservationStatus.CONFIRMED.name(), operatorId, "后台确认预约");
        entity.setStatus(ReservationStatus.CONFIRMED.name());
        log.info("预约已确认: reserveNo={}, operatorId={}", reserveNo, operatorId);
        return toVO(entity, memberMapper.selectById(entity.getMemberId()), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO reject(Long storeId, Long operatorId, String reserveNo, String remark) {
        Reservation entity = requireStore(storeId, reserveNo);
        ReservationStatusTransition.require(entity.getStatus(), ReservationStatus.CANCELLED.name());
        if (reservationMapper.cancel(entity.getId()) == 0) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        releaseSlot(entity);
        writeLog(entity.getId(), entity.getStatus(), ReservationStatus.CANCELLED.name(), operatorId,
                "后台拒绝：" + remark);
        entity.setStatus(ReservationStatus.CANCELLED.name());
        log.info("预约已拒绝: reserveNo={}, operatorId={}", reserveNo, operatorId);
        return toVO(entity, memberMapper.selectById(entity.getMemberId()), LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO checkin(Long storeId, Long operatorId, String reserveNo, Long tableId) {
        Reservation entity = requireStore(storeId, reserveNo);
        ReservationStatusTransition.require(entity.getStatus(), ReservationStatus.CHECKED_IN.name());
        // 创建时已锁定具体包间的预约不允许签到改绑，否则后台 / 扫码签到的 tableId 会覆盖锁定值，
        // 且绕过该时段包间占用校验；只有未锁定的普通预约才接受签到时绑定桌台。
        Long boundTableId = entity.getTableId() != null
                ? entity.getTableId() : resolveTableId(storeId, tableId);
        LocalDateTime now = LocalDateTime.now();
        String fromStatus = entity.getStatus();
        if (reservationMapper.checkIn(entity.getId(), operatorId, now, boundTableId) == 0) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
        entity.setStatus(ReservationStatus.CHECKED_IN.name());
        entity.setCheckinUserId(operatorId);
        entity.setCheckinTime(now);
        if (boundTableId != null) {
            entity.setTableId(boundTableId);
        }
        writeLog(entity.getId(), fromStatus, ReservationStatus.CHECKED_IN.name(), operatorId,
                boundTableId == null ? "后台签到" : "后台签到并绑定桌台 " + boundTableId);
        log.info("预约已签到: reserveNo={}, operatorId={}, tableId={}", reserveNo, operatorId, boundTableId);
        return toVO(entity, memberMapper.selectById(entity.getMemberId()), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationVO checkinByCode(Long storeId, Long operatorId, ReservationCheckinDTO dto) {
        String code = dto.getCode() == null ? "" : dto.getCode().trim().toUpperCase();
        String reserveNo = parseReserveNo(code);
        String sign = code.substring(code.length() - ReservationConstants.SIGN_LENGTH);
        long epochSecond = System.currentTimeMillis() / 1000L;
        boolean matched = DynamicCodeSigner.matches(ReservationConstants.SIGN_SCOPE, reserveNo, sign,
                epochSecond, ReservationConstants.TIME_WINDOW_SECONDS, ReservationConstants.SIGN_LENGTH,
                requireCodeSecret());
        if (!matched) {
            throw new BizException(ErrorCode.RESERVATION_CODE_INVALID);
        }
        return checkin(storeId, operatorId, reserveNo, dto.getTableId());
    }

    @Override
    public List<ReservationCalendarDayVO> calendar(Long storeId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start.plusDays(reservationProperties.getDaysAhead() - 1L) : endDate;
        if (end.isBefore(start)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "结束日期不能早于开始日期");
        }
        if (Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays() >= ReservationConstants.CALENDAR_MAX_DAYS) {
            throw new BizException(ErrorCode.PARAM_INVALID, "日历查询跨度不能超过 31 天");
        }
        Map<LocalDate, ReservationCalendarDayVO> days = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            ReservationCalendarDayVO day = new ReservationCalendarDayVO();
            day.setDate(date.format(DATE_FORMATTER));
            day.setTotal(0);
            day.setPending(0);
            day.setConfirmed(0);
            day.setCheckedIn(0);
            day.setNoShow(0);
            day.setCancelled(0);
            day.setCapacity(0);
            day.setBooked(0);
            days.put(date, day);
        }
        for (ReservationSlot slot : reservationSlotService.listBetween(storeId, start, end)) {
            ReservationCalendarDayVO day = days.get(slot.getReserveDate());
            if (day == null) {
                continue;
            }
            day.setCapacity(day.getCapacity() + nullToZero(slot.getCapacity()));
            day.setBooked(day.getBooked() + nullToZero(slot.getBooked()));
        }
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStoreId, storeId)
                .ge(Reservation::getReserveDate, start)
                .le(Reservation::getReserveDate, end));
        for (Reservation entity : reservations) {
            ReservationCalendarDayVO day = days.get(entity.getReserveDate());
            if (day == null) {
                continue;
            }
            day.setTotal(day.getTotal() + 1);
            switch (entity.getStatus()) {
                case "PENDING":
                    day.setPending(day.getPending() + 1);
                    break;
                case "CONFIRMED":
                    day.setConfirmed(day.getConfirmed() + 1);
                    break;
                case "CHECKED_IN":
                    day.setCheckedIn(day.getCheckedIn() + 1);
                    break;
                case "NO_SHOW":
                    day.setNoShow(day.getNoShow() + 1);
                    break;
                case "CANCELLED":
                    day.setCancelled(day.getCancelled() + 1);
                    break;
                default:
                    break;
            }
        }
        return new ArrayList<>(days.values());
    }

    @Override
    public ReservationTableBoardVO tableBoard(Long storeId, LocalDate reserveDate) {
        LocalDate date = reserveDate == null ? LocalDate.now() : reserveDate;
        List<StoreTable> tables = storeTableMapper.selectList(new LambdaQueryWrapper<StoreTable>()
                .eq(StoreTable::getStoreId, storeId)
                .orderByAsc(StoreTable::getTableNo));
        List<Reservation> reservations = reservationMapper.selectList(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStoreId, storeId)
                .eq(Reservation::getReserveDate, date)
                .ne(Reservation::getStatus, ReservationStatus.CANCELLED.name())
                .orderByAsc(Reservation::getTimeSlot));
        Map<Long, Long> boundCount = reservations.stream()
                .filter(item -> item.getTableId() != null)
                .collect(Collectors.groupingBy(Reservation::getTableId, Collectors.counting()));
        Map<String, String> nextSlotByType = new HashMap<>(8);
        Map<String, Long> countByType = reservations.stream()
                .collect(Collectors.groupingBy(Reservation::getTableType, Collectors.counting()));
        boolean todayBoard = date.equals(LocalDate.now());
        LocalTime nowTime = LocalTime.now();
        for (Reservation item : reservations) {
            // 当天看板只把未过去的时段当作「下一预约」
            if (todayBoard && LocalTime.parse(item.getTimeSlot(), TIME_FORMATTER).isBefore(nowTime)) {
                continue;
            }
            nextSlotByType.putIfAbsent(item.getTableType(), item.getTimeSlot());
        }
        List<TableBoardItemVO> items = new ArrayList<>();
        for (StoreTable table : tables) {
            TableBoardItemVO item = new TableBoardItemVO();
            item.setTableId(table.getId());
            item.setTableNo(table.getTableNo());
            item.setTableType(table.getTableType());
            item.setTableTypeText(ReservationTableType.describe(table.getTableType()));
            item.setCapacity(table.getCapacity());
            item.setStatus(table.getStatus());
            item.setStatusText(describeTableStatus(table.getStatus()));
            item.setReservationCount(boundCount.getOrDefault(table.getId(), 0L).intValue());
            item.setNextTimeSlot(nextSlotByType.get(table.getTableType()));
            items.add(item);
        }
        ReservationTableBoardVO vo = new ReservationTableBoardVO();
        vo.setDate(date.format(DATE_FORMATTER));
        vo.setTables(items);
        vo.setSlots(reservationSlotService.listByDate(storeId, date).stream()
                .map(ReservationConverter::toSlotVO).collect(Collectors.toList()));
        Map<Long, Member> members = loadMembers(reservations);
        String storeName = storeName(reservations);
        vo.setReservations(reservations.stream()
                .map(entity -> toVO(entity, members.get(entity.getMemberId()), LocalDateTime.now(), storeName))
                .collect(Collectors.toList()));
        log.debug("桌台看板查询完成: date={}, tables={}, typeCount={}", date, items.size(), countByType.size());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markNoShow(String reserveNo, long expectedReserveAtMillis) {
        Reservation entity = reservationMapper.selectByReserveNo(reserveNo);
        if (entity == null) {
            log.info("超时消息对应的预约不存在，已忽略: reserveNo={}", reserveNo);
            return false;
        }
        LocalDateTime reserveAt = ReservationConverter.reserveAt(entity);
        if (reserveAt == null || toMillis(reserveAt) != expectedReserveAtMillis) {
            // 改期后旧延迟消息必然到点，按预约时刻不一致直接丢弃
            log.info("预约已改期，旧超时消息丢弃: reserveNo={}", reserveNo);
            return false;
        }
        if (!ReservationStatus.active(entity.getStatus())) {
            return false;
        }
        // 防御提前到达的消息（补发探针 / 时钟漂移）：未到「预约时刻 + 超时分钟」不得释放容量
        if (LocalDateTime.now().isBefore(reserveAt.plusMinutes(reservationProperties.getNoShowMinutes()))) {
            log.info("未到超时时刻，忽略提前到达的预约超时消息: reserveNo={}", reserveNo);
            return false;
        }
        if (reservationMapper.markNoShow(entity.getId()) == 0) {
            return false;
        }
        releaseSlot(entity);
        writeLog(entity.getId(), entity.getStatus(), ReservationStatus.NO_SHOW.name(), null,
                "系统：超时未签到，自动释放时段");
        log.info("预约超时未签到已置为 NO_SHOW: reserveNo={}", reserveNo);
        return true;
    }

    @Override
    public boolean shouldRemind(String reserveNo, long expectedReserveAtMillis) {
        Reservation entity = reservationMapper.selectByReserveNo(reserveNo);
        if (entity == null || !ReservationStatus.active(entity.getStatus())) {
            return false;
        }
        LocalDateTime reserveAt = ReservationConverter.reserveAt(entity);
        return reserveAt != null && toMillis(reserveAt) == expectedReserveAtMillis;
    }

    @Override
    public void sendProbe(Long storeId, String reserveNo, String kind, long delaySeconds) {
        Reservation entity = requireStore(storeId, reserveNo);
        long bounded = Math.max(1L, Math.min(delaySeconds, PROBE_MAX_DELAY_SECONDS));
        LocalDateTime reserveAt = ReservationConverter.reserveAt(entity);
        if (reserveAt == null) {
            throw new BizException(ErrorCode.RESERVATION_TIME_INVALID);
        }
        if (KIND_REMIND.equals(kind)) {
            publish(eventMessage(entity, ReservationMessage.TYPE_REMIND, reserveAt),
                    RabbitConst.RK_RESERVATION_REMIND, bounded * 1000L);
            return;
        }
        publish(eventMessage(entity, ReservationMessage.TYPE_TIMEOUT_NO_SHOW, reserveAt),
                RabbitConst.RK_RESERVATION_TIMEOUT, bounded * 1000L);
    }

    private void insertWithRetry(Reservation entity) {
        for (int attempt = 0; attempt < ReservationConstants.RESERVE_NO_MAX_RETRY; attempt++) {
            entity.setReserveNo(ReservationNoGenerator.next());
            try {
                reservationMapper.insert(entity);
                return;
            } catch (DuplicateKeyException e) {
                log.warn("预约单号冲突，重试: reserveNo={}", entity.getReserveNo());
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "预约单号生成冲突次数过多");
    }

    /**
     * 指定包间的校验与锁定。未指定 tableId 时只校验 roomType 是否合法，仍走原来的桌型容量桶。
     *
     * <p>并发口径：同一包间同一时段的占用靠查询 active 预约判定，属于应用层校验；
     * 没有给 reservation 加唯一索引，极端并发下两笔预约可能同时通过，见 init.md 已知局限。</p>
     */
    private Long resolveRoomTable(Long storeId, ReservationCreateDTO dto) {
        if (StringUtils.hasText(dto.getRoomType()) && !ReservationRoomType.isValid(dto.getRoomType())) {
            throw new BizException(ErrorCode.TABLE_ROOM_TYPE_INVALID);
        }
        if (dto.getTableId() == null) {
            return null;
        }
        StoreTable table = storeTableMapper.selectById(dto.getTableId());
        if (table == null || !Objects.equals(table.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.STORE_TABLE_NOT_FOUND);
        }
        if (!ReservationRoomType.isPrivate(table.getRoomType())
                || !Objects.equals(dto.getTableType(), table.getRoomType())
                || (StringUtils.hasText(dto.getRoomType())
                && !dto.getRoomType().equals(table.getRoomType()))) {
            throw new BizException(ErrorCode.TABLE_ROOM_TYPE_INVALID);
        }
        if (table.getCapacity() == null || table.getCapacity() < dto.getPeopleCount()) {
            throw new BizException(ErrorCode.RESERVATION_PEOPLE_INVALID);
        }
        List<Long> occupied = reservationMapper.selectOccupiedTableIds(storeId,
                dto.getReserveDate(), dto.getTimeSlot());
        if (occupied != null && occupied.contains(dto.getTableId())) {
            throw new BizException(ErrorCode.RESERVATION_SLOT_FULL, "该包间在所选时段已被预约");
        }
        return dto.getTableId();
    }

    private void validateBookable(LocalDate reserveDate, String timeSlot, String tableType, int peopleCount) {
        requireBookableDate(reserveDate);
        if (!reservationProperties.getTimeSlots().contains(timeSlot)) {
            throw new BizException(ErrorCode.RESERVATION_SLOT_NOT_FOUND);
        }
        if (!reservationProperties.getTableTypes().contains(tableType)) {
            throw new BizException(ErrorCode.RESERVATION_TABLE_TYPE_INVALID);
        }
        if (!ReservationTableType.supports(tableType, peopleCount)) {
            throw new BizException(ErrorCode.RESERVATION_PEOPLE_INVALID);
        }
        LocalDateTime reserveAt = LocalDateTime.of(reserveDate, LocalTime.parse(timeSlot, TIME_FORMATTER));
        if (!reserveAt.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.RESERVATION_TIME_INVALID);
        }
    }

    private void requireBookableDate(LocalDate reserveDate) {
        if (reserveDate == null) {
            throw new BizException(ErrorCode.RESERVATION_TIME_INVALID);
        }
        LocalDate today = LocalDate.now();
        LocalDate lastDate = today.plusDays(reservationProperties.getDaysAhead() - 1L);
        if (reserveDate.isBefore(today) || reserveDate.isAfter(lastDate)) {
            throw new BizException(ErrorCode.RESERVATION_TIME_INVALID,
                    "仅支持预约 " + today + " 至 " + lastDate + " 的时段");
        }
    }

    private void releaseSlot(Reservation entity) {
        ReservationSlot slot = reservationSlotService.findSlot(entity.getStoreId(), entity.getReserveDate(),
                entity.getTimeSlot(), entity.getTableType());
        if (slot == null) {
            log.warn("预约时段不存在，跳过容量释放: reserveNo={}", entity.getReserveNo());
            return;
        }
        reservationSlotService.releaseInTransaction(slot.getId());
    }

    private void writeLog(Long reservationId, String fromStatus, String toStatus, Long operatorId, String remark) {
        ReservationStatusLog record = new ReservationStatusLog();
        record.setReservationId(reservationId);
        record.setFromStatus(fromStatus == null ? "" : fromStatus);
        record.setToStatus(toStatus);
        record.setOperatorId(operatorId);
        record.setRemark(remark == null ? "" : remark);
        reservationStatusLogMapper.insert(record);
    }

    private List<com.comeoften.module.reservation.vo.ReservationStatusLogVO> listLogs(Long reservationId) {
        return reservationStatusLogMapper.selectList(new LambdaQueryWrapper<ReservationStatusLog>()
                        .eq(ReservationStatusLog::getReservationId, reservationId)
                        .orderByAsc(ReservationStatusLog::getCreateTime))
                .stream().map(ReservationConverter::toLogVO).collect(Collectors.toList());
    }

    private Reservation requireOwned(Long memberId, String reserveNo) {
        Reservation entity = reservationMapper.selectByReserveNo(reserveNo);
        if (entity == null) {
            throw new BizException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!Objects.equals(entity.getMemberId(), memberId)) {
            throw new BizException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }
        return entity;
    }

    private Reservation requireStore(Long storeId, String reserveNo) {
        Reservation entity = reservationMapper.selectByReserveNo(reserveNo);
        if (entity == null) {
            throw new BizException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        if (!Objects.equals(entity.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }
        return entity;
    }

    private Store requireStoreEntity(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new BizException(ErrorCode.STORE_NOT_FOUND);
        }
        return store;
    }

    private Long resolveTableId(Long storeId, Long tableId) {
        if (tableId == null) {
            return null;
        }
        StoreTable table = storeTableMapper.selectById(tableId);
        if (table == null || !Objects.equals(table.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "桌台不存在");
        }
        return tableId;
    }

    private void publishTimeout(Reservation entity, LocalDateTime reserveAt, long delayMillis) {
        publish(eventMessage(entity, ReservationMessage.TYPE_TIMEOUT_NO_SHOW, reserveAt),
                RabbitConst.RK_RESERVATION_TIMEOUT, delayMillis);
    }

    private void publishRemind(Reservation entity, LocalDateTime reserveAt, long delayMillis) {
        if (delayMillis <= 0L) {
            return;
        }
        publish(eventMessage(entity, ReservationMessage.TYPE_REMIND, reserveAt),
                RabbitConst.RK_RESERVATION_REMIND, delayMillis);
    }

    private void publish(ReservationMessage message, String routingKey, long delayMillis) {
        MqEnvelope envelope = new MqEnvelope();
        envelope.setMessageId(message.getMessageId());
        envelope.setExchangeName(RabbitConst.DELAY_EXCHANGE);
        envelope.setRoutingKey(routingKey);
        envelope.setDelayMillis(Math.max(MIN_DELAY_MILLIS, delayMillis));
        envelope.setBody(message);
        messageProducer.publish(envelope);
    }

    private ReservationMessage eventMessage(Reservation entity, String eventType, LocalDateTime reserveAt) {
        ReservationMessage message = new ReservationMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setEventType(eventType);
        message.setOccurredAtMillis(System.currentTimeMillis());
        message.setReserveNo(entity.getReserveNo());
        message.setReservationId(entity.getId());
        message.setMemberId(entity.getMemberId());
        message.setStoreId(entity.getStoreId());
        message.setReserveDate(entity.getReserveDate() == null ? ""
                : entity.getReserveDate().format(DATE_FORMATTER));
        message.setTimeSlot(entity.getTimeSlot());
        message.setTableType(entity.getTableType());
        message.setReserveAtMillis(toMillis(reserveAt));
        return message;
    }

    private long timeoutDelayMillis(LocalDateTime reserveAt) {
        long delay = Duration.between(LocalDateTime.now(),
                reserveAt.plusMinutes(reservationProperties.getNoShowMinutes())).toMillis();
        return Math.max(MIN_DELAY_MILLIS, delay);
    }

    private long remindDelayMillis(LocalDateTime reserveAt) {
        return Duration.between(LocalDateTime.now(),
                reserveAt.minusMinutes(reservationProperties.getRemindAheadMinutes())).toMillis();
    }

    private String parseReserveNo(String code) {
        if (!StringUtils.hasText(code) || !code.startsWith(ReservationConstants.CODE_PREFIX)
                || code.length() <= ReservationConstants.CODE_PREFIX.length() + ReservationConstants.SIGN_LENGTH) {
            throw new BizException(ErrorCode.RESERVATION_CODE_INVALID);
        }
        return code.substring(ReservationConstants.CODE_PREFIX.length(),
                code.length() - ReservationConstants.SIGN_LENGTH);
    }

    private String requireCodeSecret() {
        if (!StringUtils.hasText(couponProperties.getCodeSecret())) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "签到码密钥未配置");
        }
        return couponProperties.getCodeSecret();
    }

    private ReservationVO toVO(Reservation entity, Member member, LocalDateTime now) {
        return toVO(entity, member, now, storeNameOf(entity.getStoreId()));
    }

    private ReservationVO toVO(Reservation entity, Member member, LocalDateTime now, String storeName) {
        return ReservationConverter.toVO(entity, storeName, member,
                reservationProperties.getCancelLimitHours(), now);
    }

    private Map<Long, Member> loadMembers(List<Reservation> reservations) {
        Set<Long> memberIds = new HashSet<>();
        for (Reservation entity : reservations) {
            if (entity.getMemberId() != null) {
                memberIds.add(entity.getMemberId());
            }
        }
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return memberMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, item -> item, (left, right) -> left));
    }

    private String storeName(List<Reservation> reservations) {
        return reservations.isEmpty() ? null : storeNameOf(reservations.get(0).getStoreId());
    }

    private String storeNameOf(Long storeId) {
        Store store = storeId == null ? null : storeMapper.selectById(storeId);
        return store == null ? null : store.getStoreName();
    }

    private Map<String, TableTypeStatVO> statMap(Long storeId) {
        return reservationSlotService.statTableType(storeId).stream()
                .collect(Collectors.toMap(TableTypeStatVO::getTableType, item -> item, (left, right) -> left));
    }

    private static String describeTableStatus(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 1:
                return "占用";
            case 2:
                return "已预约";
            default:
                return "空闲";
        }
    }

    private static long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static long boundSize(Long size) {
        long value = size == null ? 10L : size;
        return Math.max(1L, Math.min(value, PAGE_MAX_SIZE));
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
