package com.comeoften.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.config.MemberProperties;
import com.comeoften.module.member.constant.MemberTagCode;
import com.comeoften.module.member.entity.MemberTag;
import com.comeoften.module.member.mapper.MemberTagMapper;
import com.comeoften.module.member.service.MemberTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员标签实现。沉睡阈值只有一处来源：{@code comeoften.member.sleeping-days}。
 *
 * <p>打标判据是「最近一次支付成功订单早于阈值」，没有支付历史的会员用注册时间判断；
 * 两条都要有，否则刚注册还没下单的新客会被误标成沉睡。回访（阈值内有支付单）当天摘标，
 * 让「沉睡 → 唤醒券 → 回访」形成闭环，而不是打上一次永久挂着。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTagServiceImpl implements MemberTagService {

    private final MemberTagMapper memberTagMapper;

    private final MemberProperties memberProperties;

    @Override
    public Map<Long, List<String>> tagNamesOf(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> result = new HashMap<>(memberIds.size());
        for (MemberTag tag : memberTagMapper.selectList(new LambdaQueryWrapper<MemberTag>()
                .in(MemberTag::getMemberId, memberIds))) {
            result.computeIfAbsent(tag.getMemberId(), key -> new ArrayList<>()).add(tag.getTagName());
        }
        return result;
    }

    @Override
    public List<Long> memberIdsByTag(String tagCode, int limit) {
        return memberTagMapper.selectMemberIdsByTag(tagCode, Math.max(1, limit));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int refreshSleeping() {
        int batchSize = Math.max(1, memberProperties.getTagBatchSize());
        LocalDateTime from = LocalDateTime.now().minusDays(Math.max(1, memberProperties.getSleepingDays()));
        List<Long> revived = memberTagMapper.selectRevivedTagIds(MemberTagCode.SLEEPING, from, batchSize);
        int cleared = 0;
        for (Long id : revived) {
            // 逻辑删除：uk_member_tag 会保留行，下次再沉睡时靠 revive 复活
            if (memberTagMapper.deleteById(id) > 0) {
                cleared++;
            }
        }
        List<Long> candidates = memberTagMapper.selectSleepingCandidates(MemberTagCode.SLEEPING,
                from, batchSize);
        int marked = 0;
        for (Long memberId : candidates) {
            if (markSleeping(memberId)) {
                marked++;
            }
        }
        if (marked > 0 || cleared > 0) {
            log.info("沉睡标签刷新完成: threshold={}, marked={}, cleared={}",
                    from, marked, cleared);
        }
        return marked + cleared;
    }

    private boolean markSleeping(Long memberId) {
        MemberTag tag = new MemberTag();
        tag.setMemberId(memberId);
        tag.setTagCode(MemberTagCode.SLEEPING);
        tag.setTagName(MemberTagCode.SLEEPING_NAME);
        try {
            memberTagMapper.insert(tag);
            return true;
        } catch (DuplicateKeyException e) {
            // 逻辑删除过的行仍占唯一索引，复活而不是再插一条
            return memberTagMapper.revive(memberId, MemberTagCode.SLEEPING) > 0;
        }
    }
}
