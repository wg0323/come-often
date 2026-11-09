package com.comeoften.module.queue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.MaskUtils;
import com.comeoften.common.PageResult;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.queue.constant.QueueTicketStatus;
import com.comeoften.module.queue.converter.QueueTicketConverter;
import com.comeoften.module.queue.entity.QueueTicket;
import com.comeoften.module.queue.mapper.QueueTicketMapper;
import com.comeoften.module.queue.service.QueueTicketService;
import com.comeoften.module.queue.vo.QueueTicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 排队叫号实现。编号按门店 + 营业日用 Redis 自增，DB 唯一索引兜底；
 * 状态推进一律用带旧状态条件的 UPDATE，重复点击不会把号推过状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueTicketServiceImpl implements QueueTicketService {

    private static final String STATUS_WAITING = QueueTicketStatus.WAITING.name();

    private static final String STATUS_CALLED = QueueTicketStatus.CALLED.name();

    private static final String STATUS_SEATED = QueueTicketStatus.SEATED.name();

    private static final String STATUS_CANCELLED = QueueTicketStatus.CANCELLED.name();

    private static final List<String> ACTIVE_STATUS = Arrays.asList(STATUS_WAITING, STATUS_CALLED);

    private static final List<String> CANCELLABLE_STATUS = Arrays.asList(STATUS_WAITING, STATUS_CALLED);

    private static final String TICKET_NO_FORMAT = "Q%03d";

    private static final String QUEUE_KEY_PREFIX = "comeoften:queue:no:";

    private static final DateTimeFormatter DATE_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final int KEY_TTL_HOURS = 48;

    private static final int MAX_INSERT_RETRY = 3;

    private static final int MIN_PARTY_SIZE = 1;

    private static final int MAX_PARTY_SIZE = 20;

    private static final long PAGE_MAX_SIZE = 100L;

    private final QueueTicketMapper queueTicketMapper;

    private final MemberMapper memberMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueueTicketVO take(Long memberId, Long storeId, Integer partySize) {
        if (partySize == null || partySize < MIN_PARTY_SIZE || partySize > MAX_PARTY_SIZE) {
            throw new BizException(ErrorCode.QUEUE_PARTY_SIZE_INVALID);
        }
        LocalDate today = LocalDate.now();
        Long active = queueTicketMapper.selectCount(new LambdaQueryWrapper<QueueTicket>()
                .eq(QueueTicket::getStoreId, storeId)
                .eq(QueueTicket::getMemberId, memberId)
                .eq(QueueTicket::getBizDate, today)
                .in(QueueTicket::getStatus, ACTIVE_STATUS));
        if (active != null && active > 0L) {
            throw new BizException(ErrorCode.QUEUE_TICKET_DUPLICATED);
        }
        QueueTicket ticket = new QueueTicket();
        ticket.setStoreId(storeId);
        ticket.setMemberId(memberId);
        ticket.setBizDate(today);
        ticket.setPartySize(partySize);
        ticket.setStatus(STATUS_WAITING);
        for (int attempt = 0; attempt < MAX_INSERT_RETRY; attempt++) {
            ticket.setId(null);
            ticket.setTicketNo(nextTicketNo(storeId, today, attempt));
            try {
                queueTicketMapper.insert(ticket);
                log.info("顾客取号成功: storeId={}, memberId={}, ticketNo={}, partySize={}",
                        storeId, memberId, ticket.getTicketNo(), partySize);
                return QueueTicketConverter.toVO(ticket, waitingCount(ticket), "");
            } catch (DuplicateKeyException exception) {
                log.warn("排队号冲突，重新生成: storeId={}, ticketNo={}", storeId, ticket.getTicketNo(), exception);
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "取号失败，请稍后重试");
    }

    @Override
    public QueueTicketVO current(Long memberId, Long storeId) {
        QueueTicket ticket = queueTicketMapper.selectOne(new LambdaQueryWrapper<QueueTicket>()
                .eq(QueueTicket::getStoreId, storeId)
                .eq(QueueTicket::getMemberId, memberId)
                .eq(QueueTicket::getBizDate, LocalDate.now())
                .in(QueueTicket::getStatus, ACTIVE_STATUS)
                .orderByDesc(QueueTicket::getId)
                .last("LIMIT 1"));
        return ticket == null ? null : QueueTicketConverter.toVO(ticket, waitingCount(ticket), "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueueTicketVO cancel(Long memberId, Long storeId, String ticketNo) {
        QueueTicket ticket = requireTicket(storeId, ticketNo);
        if (!memberId.equals(ticket.getMemberId())) {
            throw new BizException(ErrorCode.QUEUE_TICKET_NOT_FOUND);
        }
        int rows = queueTicketMapper.update(null, new LambdaUpdateWrapper<QueueTicket>()
                .eq(QueueTicket::getId, ticket.getId())
                .in(QueueTicket::getStatus, CANCELLABLE_STATUS)
                .set(QueueTicket::getStatus, STATUS_CANCELLED));
        if (rows == 0) {
            throw new BizException(ErrorCode.QUEUE_TICKET_STATUS_INVALID);
        }
        ticket.setStatus(STATUS_CANCELLED);
        return QueueTicketConverter.toVO(ticket, 0L, "");
    }

    @Override
    public PageResult<QueueTicketVO> page(Long storeId, String status, Long current, Long size) {
        long pageCurrent = Math.max(1L, current == null ? 1L : current);
        long pageSize = Math.min(PAGE_MAX_SIZE, Math.max(1L, size == null ? 10L : size));
        Page<QueueTicket> page = new Page<>(pageCurrent, pageSize);
        IPage<QueueTicket> result = queueTicketMapper.selectPage(page,
                new LambdaQueryWrapper<QueueTicket>()
                        .eq(QueueTicket::getStoreId, storeId)
                        .eq(QueueTicket::getBizDate, LocalDate.now())
                        .eq(StringUtils.hasText(status), QueueTicket::getStatus, status)
                        .orderByAsc(QueueTicket::getId));
        Map<Long, String> phones = loadPhones(result.getRecords());
        List<QueueTicketVO> records = result.getRecords().stream()
                .map(ticket -> QueueTicketConverter.toVO(ticket, waitingCount(ticket),
                        phones.get(ticket.getMemberId())))
                .collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueueTicketVO call(Long storeId, String ticketNo) {
        QueueTicket ticket = requireTicket(storeId, ticketNo);
        int rows = queueTicketMapper.update(null, new LambdaUpdateWrapper<QueueTicket>()
                .eq(QueueTicket::getId, ticket.getId())
                .eq(QueueTicket::getStatus, STATUS_WAITING)
                .set(QueueTicket::getStatus, STATUS_CALLED)
                .set(QueueTicket::getCalledTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.QUEUE_TICKET_STATUS_INVALID);
        }
        log.info("排队号已叫号: storeId={}, ticketNo={}", storeId, ticketNo);
        return reload(storeId, ticketNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueueTicketVO seat(Long storeId, String ticketNo) {
        QueueTicket ticket = requireTicket(storeId, ticketNo);
        int rows = queueTicketMapper.update(null, new LambdaUpdateWrapper<QueueTicket>()
                .eq(QueueTicket::getId, ticket.getId())
                .eq(QueueTicket::getStatus, STATUS_CALLED)
                .set(QueueTicket::getStatus, STATUS_SEATED)
                .set(QueueTicket::getSeatedTime, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.QUEUE_TICKET_STATUS_INVALID);
        }
        log.info("排队号已入座: storeId={}, ticketNo={}", storeId, ticketNo);
        return reload(storeId, ticketNo);
    }

    private QueueTicket requireTicket(Long storeId, String ticketNo) {
        if (!StringUtils.hasText(ticketNo)) {
            throw new BizException(ErrorCode.QUEUE_TICKET_NOT_FOUND);
        }
        QueueTicket ticket = queueTicketMapper.selectOne(new LambdaQueryWrapper<QueueTicket>()
                .eq(QueueTicket::getStoreId, storeId)
                .eq(QueueTicket::getBizDate, LocalDate.now())
                .eq(QueueTicket::getTicketNo, ticketNo)
                .last("LIMIT 1"));
        if (ticket == null) {
            throw new BizException(ErrorCode.QUEUE_TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private QueueTicketVO reload(Long storeId, String ticketNo) {
        QueueTicket ticket = requireTicket(storeId, ticketNo);
        return QueueTicketConverter.toVO(ticket, waitingCount(ticket), "");
    }

    private String nextTicketNo(Long storeId, LocalDate date, int offset) {
        long sequence;
        String key = QUEUE_KEY_PREFIX + storeId + ":" + date.format(DATE_KEY_FORMAT);
        try {
            Long increment = stringRedisTemplate.opsForValue().increment(key);
            if (increment != null && increment == 1L) {
                stringRedisTemplate.expire(key, KEY_TTL_HOURS, TimeUnit.HOURS);
            }
            sequence = increment == null ? fallbackSequence(storeId, date, offset) : increment;
        } catch (DataAccessException exception) {
            log.warn("排队号 Redis 自增不可用，按 DB 现有数量兜底: key={}", key, exception);
            sequence = fallbackSequence(storeId, date, offset);
        }
        return String.format(TICKET_NO_FORMAT, sequence);
    }

    private long fallbackSequence(Long storeId, LocalDate date, int offset) {
        Long count = queueTicketMapper.selectCount(new LambdaQueryWrapper<QueueTicket>()
                .eq(QueueTicket::getStoreId, storeId)
                .eq(QueueTicket::getBizDate, date));
        return (count == null ? 0L : count) + 1L + offset;
    }

    private Long waitingCount(QueueTicket ticket) {
        if (!STATUS_WAITING.equals(ticket.getStatus())) {
            return 0L;
        }
        Long count = queueTicketMapper.selectCount(new LambdaQueryWrapper<QueueTicket>()
                .eq(QueueTicket::getStoreId, ticket.getStoreId())
                .eq(QueueTicket::getBizDate, ticket.getBizDate())
                .eq(QueueTicket::getStatus, STATUS_WAITING)
                .lt(QueueTicket::getId, ticket.getId()));
        return count == null ? 0L : count;
    }

    private Map<Long, String> loadPhones(List<QueueTicket> tickets) {
        List<Long> memberIds = tickets.stream().map(QueueTicket::getMemberId).distinct()
                .collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Member> members = memberMapper.selectBatchIds(memberIds);
        Map<Long, String> phones = new HashMap<>(members.size());
        for (Member member : members) {
            phones.put(member.getId(), MaskUtils.maskPhone(member.getPhone()));
        }
        return phones;
    }
}
