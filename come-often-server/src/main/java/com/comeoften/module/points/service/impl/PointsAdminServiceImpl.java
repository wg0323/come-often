package com.comeoften.module.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.PageResult;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.member.service.MemberTagService;
import com.comeoften.module.points.converter.PointsConverter;
import com.comeoften.module.points.entity.MemberLevel;
import com.comeoften.module.points.entity.PointsAccount;
import com.comeoften.module.points.entity.PointsRecord;
import com.comeoften.module.points.mapper.MemberLevelMapper;
import com.comeoften.module.points.mapper.PointsAccountMapper;
import com.comeoften.module.points.mapper.PointsRecordMapper;
import com.comeoften.module.points.service.PointsAdminService;
import com.comeoften.module.points.vo.PointsMemberVO;
import com.comeoften.module.points.vo.PointsRecordVO;
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
 * PC 后台积分查询。会员、账户、等级分三次批量取回后内存拼装，
 * 不做三表 join（member.level_id 无索引，db-spec 要求被关联字段必须有索引）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsAdminServiceImpl implements PointsAdminService {

    private final MemberMapper memberMapper;

    private final PointsAccountMapper pointsAccountMapper;

    private final PointsRecordMapper pointsRecordMapper;

    private final MemberLevelMapper memberLevelMapper;

    private final MemberTagService memberTagService;

    /** 标签筛选一次最多取多少会员；一期单店规模远小于该值。 */
    private static final int TAG_FILTER_LIMIT = 1000;

    @Override
    public PageResult<PointsMemberVO> memberPage(Long current, Long size, String keyword) {
        return memberPage(current, size, keyword, null);
    }

    @Override
    public PageResult<PointsMemberVO> memberPage(Long current, Long size, String keyword,
                                                 String tagCode) {
        List<Long> taggedMemberIds = StringUtils.hasText(tagCode)
                ? memberTagService.memberIdsByTag(tagCode, TAG_FILTER_LIMIT) : null;
        if (taggedMemberIds != null && taggedMemberIds.isEmpty()) {
            return PageResult.of(0L, current, size, Collections.emptyList());
        }
        IPage<Member> page = memberMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Member>()
                        .in(taggedMemberIds != null, Member::getId, taggedMemberIds)
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
        Map<Long, PointsAccount> accountMap = pointsAccountMapper.selectList(
                        new LambdaQueryWrapper<PointsAccount>().in(PointsAccount::getMemberId, memberIds))
                .stream().collect(Collectors.toMap(PointsAccount::getMemberId, Function.identity(),
                        (left, right) -> left));
        Map<Long, MemberLevel> levelMap = loadLevelMap(members);
        Map<Long, List<String>> tagMap = memberTagService.tagNamesOf(memberIds);
        List<PointsMemberVO> records = members.stream()
                .map(member -> {
                    PointsMemberVO vo = PointsConverter.toMemberVO(member,
                            accountMap.get(member.getId()), levelMap.get(member.getLevelId()));
                    vo.setTags(tagMap.getOrDefault(member.getId(), Collections.emptyList()));
                    return vo;
                })
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public PageResult<PointsRecordVO> recordPage(Long memberId, Long current, Long size) {
        IPage<PointsRecord> page = pointsRecordMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(memberId != null, PointsRecord::getMemberId, memberId)
                        .orderByDesc(PointsRecord::getCreateTime));
        List<PointsRecordVO> records = page.getRecords().stream()
                .map(PointsConverter::toRecordVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    private Map<Long, MemberLevel> loadLevelMap(List<Member> members) {
        List<Long> levelIds = members.stream().map(Member::getLevelId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (levelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return memberLevelMapper.selectBatchIds(levelIds).stream()
                .collect(Collectors.toMap(MemberLevel::getId, Function.identity(), (left, right) -> left));
    }
}
