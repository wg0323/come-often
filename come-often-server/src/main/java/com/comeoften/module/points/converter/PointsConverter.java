package com.comeoften.module.points.converter;

import com.comeoften.common.MaskUtils;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.points.constant.PointsChangeType;
import com.comeoften.module.points.entity.MemberLevel;
import com.comeoften.module.points.entity.PointsAccount;
import com.comeoften.module.points.entity.PointsRecord;
import com.comeoften.module.points.vo.MemberLevelVO;
import com.comeoften.module.points.vo.PointsAccountVO;
import com.comeoften.module.points.vo.PointsMemberVO;
import com.comeoften.module.points.vo.PointsRecordVO;
import org.springframework.beans.BeanUtils;

/**
 * 积分域对象转换。同名同类型字段交给 BeanUtils 一次拷贝，只显式处理字段名不同
 * （level.id → levelId、member.phone → phoneMasked）与码值翻译（changeTypeText）。
 */
public final class PointsConverter {

    private PointsConverter() {
    }

    public static PointsAccountVO toAccountVO(PointsAccount account, MemberLevel level) {
        PointsAccountVO vo = new PointsAccountVO();
        if (account != null) {
            BeanUtils.copyProperties(account, vo);
        } else {
            // 从未产生过积分流水的会员没有账户行，接口仍返回零值而非报错
            vo.setBalance(0);
            vo.setTotalEarned(0);
            vo.setTotalUsed(0);
        }
        vo.setLevelId(level == null ? null : level.getId());
        vo.setLevelCode(level == null ? "" : level.getLevelCode());
        vo.setLevelName(level == null ? "" : level.getLevelName());
        vo.setDiscountRate(level == null ? null : level.getDiscountRate());
        return vo;
    }

    public static PointsRecordVO toRecordVO(PointsRecord record) {
        PointsRecordVO vo = new PointsRecordVO();
        BeanUtils.copyProperties(record, vo);
        PointsChangeType type = PointsChangeType.of(record.getChangeType());
        vo.setChangeTypeText(type == null ? record.getChangeType() : type.getDescription());
        return vo;
    }

    public static MemberLevelVO toLevelVO(MemberLevel level) {
        MemberLevelVO vo = new MemberLevelVO();
        BeanUtils.copyProperties(level, vo);
        vo.setLevelId(level.getId());
        return vo;
    }

    public static PointsMemberVO toMemberVO(Member member, PointsAccount account, MemberLevel level) {
        PointsMemberVO vo = new PointsMemberVO();
        if (account != null) {
            BeanUtils.copyProperties(account, vo);
        } else {
            vo.setBalance(0);
            vo.setTotalEarned(0);
            vo.setTotalUsed(0);
        }
        if (member != null) {
            vo.setMemberId(member.getId());
            vo.setNickname(member.getNickname());
            vo.setPhoneMasked(MaskUtils.maskPhone(member.getPhone()));
        }
        vo.setLevelId(level == null ? null : level.getId());
        vo.setLevelCode(level == null ? "" : level.getLevelCode());
        vo.setLevelName(level == null ? "" : level.getLevelName());
        vo.setDiscountRate(level == null ? null : level.getDiscountRate());
        return vo;
    }
}
