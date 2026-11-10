package com.comeoften.module.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.points.converter.PointsConverter;
import com.comeoften.module.points.dto.MemberLevelSaveDTO;
import com.comeoften.module.points.entity.MemberLevel;
import com.comeoften.module.points.entity.PointsAccount;
import com.comeoften.module.points.mapper.MemberLevelMapper;
import com.comeoften.module.points.mapper.PointsAccountMapper;
import com.comeoften.module.points.service.PointsLevelService;
import com.comeoften.module.points.util.MemberLevelResolver;
import com.comeoften.module.points.vo.MemberLevelOverviewVO;
import com.comeoften.module.points.vo.MemberLevelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员等级实现。
 *
 * <p>门槛判定只用 member_level 的数据：年度消费额取本自然年已支付且未取消 / 未退款的实付总额，
 * 累计积分取 points_account.total_earned；两项同时达标才升级。升降级只在订单完成消费里算，
 * 不单开定时任务（计划 3.7），且只升不降——降级涉及已发权益，计划未定义，M6 不擅自放开。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsLevelServiceImpl implements PointsLevelService {

    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2);

    private final MemberLevelMapper memberLevelMapper;

    private final PointsAccountMapper pointsAccountMapper;

    private final MemberMapper memberMapper;

    private final TradeOrderMapper tradeOrderMapper;

    @Override
    public BigDecimal discountRateOf(Long memberId) {
        if (memberId == null) {
            return BigDecimal.ONE;
        }
        Member member = memberMapper.selectById(memberId);
        MemberLevel level = resolveCurrent(member, loadLevels());
        if (level == null || level.getDiscountRate() == null) {
            return BigDecimal.ONE;
        }
        return level.getDiscountRate();
    }

    @Override
    public MemberLevelOverviewVO overview(Long memberId) {
        List<MemberLevel> levels = loadLevels();
        Member member = memberMapper.selectById(memberId);
        PointsAccount account = pointsAccountMapper.selectByMemberId(memberId);
        BigDecimal annualAmount = annualAmount(memberId);
        int totalEarned = account == null || account.getTotalEarned() == null ? 0 : account.getTotalEarned();
        MemberLevel current = resolveCurrent(member, levels);
        MemberLevel next = resolveNext(current, levels);

        MemberLevelOverviewVO vo = new MemberLevelOverviewVO();
        vo.setLevels(levels.stream().map(PointsConverter::toLevelVO).collect(Collectors.toList()));
        vo.setCurrentLevel(current == null ? null : PointsConverter.toLevelVO(current));
        vo.setAnnualAmount(annualAmount);
        vo.setTotalEarned(totalEarned);
        vo.setNextLevel(next == null ? null : PointsConverter.toLevelVO(next));
        vo.setGapAmount(next == null ? ZERO_AMOUNT : gapAmount(next, annualAmount));
        vo.setGapPoints(next == null ? 0 : Math.max(0, next.getMinPoints() - totalEarned));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLevelVO refreshLevel(Long memberId) {
        if (memberId == null) {
            return null;
        }
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            return null;
        }
        List<MemberLevel> levels = loadLevels();
        PointsAccount account = pointsAccountMapper.selectByMemberId(memberId);
        int totalEarned = account == null || account.getTotalEarned() == null ? 0 : account.getTotalEarned();
        MemberLevel target = MemberLevelResolver.highest(levels, annualAmount(memberId), totalEarned);
        if (target == null) {
            return null;
        }
        MemberLevel current = member.getLevelId() == null ? null : memberLevelMapper.selectById(member.getLevelId());
        if (current != null && MemberLevelResolver.sortOf(target) <= MemberLevelResolver.sortOf(current)) {
            return null;
        }
        // 只升不降的守卫在 SQL 里（比较 member_level.sort），
        // 并发消费或快照过期都不会把已经升上去的等级改回来
        if (memberMapper.upgradeLevel(memberId, target.getId(),
                MemberLevelResolver.sortOf(target)) == 0) {
            return null;
        }
        log.info("会员等级已升级: memberId={}, from={}, to={}", memberId,
                current == null ? "NONE" : current.getLevelCode(), target.getLevelCode());
        return PointsConverter.toLevelVO(target);
    }

    @Override
    public List<MemberLevelVO> listLevels() {
        return loadLevels().stream().map(PointsConverter::toLevelVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberLevelVO updateLevel(Long levelId, MemberLevelSaveDTO dto) {
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (level == null) {
            throw new BizException(ErrorCode.MEMBER_LEVEL_NOT_FOUND);
        }
        if (dto.getSort() != null && dto.getSort() <= 0) {
            throw new BizException(ErrorCode.MEMBER_LEVEL_STATUS_INVALID, "等级排序值必须为正整数");
        }
        level.setLevelName(dto.getLevelName());
        level.setMinAmount(dto.getMinAmount());
        level.setMinPoints(dto.getMinPoints());
        level.setDiscountRate(dto.getDiscountRate());
        if (dto.getSort() != null) {
            level.setSort(dto.getSort());
        }
        memberLevelMapper.updateById(level);
        log.info("会员等级配置已更新: levelCode={}, minAmount={}, minPoints={}, discountRate={}",
                level.getLevelCode(), level.getMinAmount(), level.getMinPoints(), level.getDiscountRate());
        return PointsConverter.toLevelVO(level);
    }

    // ------------------------------------------------------------------ 私有逻辑

    /**
     * 全部等级，按排序值升序。
     */
    private List<MemberLevel> loadLevels() {
        return memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .orderByAsc(MemberLevel::getSort));
    }

    /**
     * 当前等级：优先取会员身上绑定的等级，未绑定或配置缺失时回落到最低档。
     */
    private MemberLevel resolveCurrent(Member member, List<MemberLevel> levels) {
        if (member != null && member.getLevelId() != null) {
            for (MemberLevel level : levels) {
                if (level.getId().equals(member.getLevelId())) {
                    return level;
                }
            }
        }
        return levels.isEmpty() ? null : levels.get(0);
    }

    /**
     * 下一档等级：排序值比当前大的最小一档。
     */
    private MemberLevel resolveNext(MemberLevel current, List<MemberLevel> levels) {
        if (levels.isEmpty()) {
            return null;
        }
        int currentSort = MemberLevelResolver.sortOf(current);
        MemberLevel next = null;
        for (MemberLevel level : levels) {
            if (MemberLevelResolver.sortOf(level) <= currentSort) {
                continue;
            }
            if (next == null || MemberLevelResolver.sortOf(level) < MemberLevelResolver.sortOf(next)) {
                next = level;
            }
        }
        return next;
    }

    private BigDecimal gapAmount(MemberLevel next, BigDecimal annualAmount) {
        BigDecimal gap = next.getMinAmount().subtract(annualAmount);
        return gap.signum() <= 0 ? ZERO_AMOUNT : gap.setScale(2);
    }

    /**
     * 本自然年有效实付总额：已取消与已退款订单由 SQL 排除，与等级门槛同一口径。
     */
    private BigDecimal annualAmount(Long memberId) {
        LocalDateTime from = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime to = from.plusYears(1);
        BigDecimal amount = tradeOrderMapper.sumPaidAmount(memberId, from, to);
        return amount == null ? ZERO_AMOUNT : amount.setScale(2);
    }
}
