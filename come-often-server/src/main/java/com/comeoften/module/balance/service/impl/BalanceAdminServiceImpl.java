package com.comeoften.module.balance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.PageResult;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.balance.converter.BalanceConverter;
import com.comeoften.module.balance.entity.BalanceRecord;
import com.comeoften.module.balance.entity.MemberBalance;
import com.comeoften.module.balance.mapper.BalanceRecordMapper;
import com.comeoften.module.balance.mapper.MemberBalanceMapper;
import com.comeoften.module.balance.service.BalanceAdminService;
import com.comeoften.module.balance.vo.BalanceMemberVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PC 后台储值查询实现。会员与账户两次批量取回后内存拼装，不做 join（与后台会员积分页同口径）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceAdminServiceImpl implements BalanceAdminService {

    private final MemberMapper memberMapper;

    private final MemberBalanceMapper memberBalanceMapper;

    private final BalanceRecordMapper balanceRecordMapper;

    @Override
    public PageResult<BalanceMemberVO> memberPage(Long current, Long size, String keyword) {
        IPage<Member> page = memberMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Member>()
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(Member::getNickname, keyword)
                                .or().like(Member::getPhone, keyword))
                        .orderByDesc(Member::getCreateTime));
        List<Member> members = page.getRecords();
        if (members.isEmpty()) {
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                    Collections.emptyList());
        }
        List<Long> memberIds = members.stream().map(Member::getId).collect(Collectors.toList());
        Map<Long, MemberBalance> accountMap = memberBalanceMapper.selectList(
                        new LambdaQueryWrapper<MemberBalance>().in(MemberBalance::getMemberId, memberIds))
                .stream().collect(Collectors.toMap(MemberBalance::getMemberId, Function.identity(),
                        (left, right) -> left));
        List<BalanceMemberVO> records = members.stream()
                .map(member -> BalanceConverter.toMemberVO(member, accountMap.get(member.getId())))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public PageResult<BalanceRecordVO> recordPage(Long memberId, Long current, Long size) {
        IPage<BalanceRecord> page = balanceRecordMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<BalanceRecord>()
                        .eq(memberId != null, BalanceRecord::getMemberId, memberId)
                        .orderByDesc(BalanceRecord::getCreateTime));
        List<BalanceRecordVO> records = page.getRecords().stream()
                .map(BalanceConverter::toRecordVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }
}
