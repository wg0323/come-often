package com.comeoften.module.balance.converter;

import com.comeoften.common.MaskUtils;
import com.comeoften.config.BalanceProperties;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.balance.entity.BalanceRecord;
import com.comeoften.module.balance.entity.MemberBalance;
import com.comeoften.module.balance.vo.BalanceAccountVO;
import com.comeoften.module.balance.vo.BalanceMemberVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 储值对象转换。金额一律两位小数，账户为空时按零值出参（新会员不进账户表也能看余额页）。
 */
public final class BalanceConverter {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private BalanceConverter() {
    }

    public static BalanceAccountVO toAccountVO(MemberBalance account, BalanceProperties properties) {
        BalanceAccountVO vo = new BalanceAccountVO();
        vo.setBalance(amount(account == null ? null : account.getBalance()));
        vo.setTotalRecharge(amount(account == null ? null : account.getTotalRecharge()));
        vo.setTotalGift(amount(account == null ? null : account.getTotalGift()));
        vo.setTotalConsume(amount(account == null ? null : account.getTotalConsume()));
        vo.setMinRecharge(properties.getMinRecharge());
        vo.setMaxRecharge(properties.getMaxRecharge());
        List<BalanceAccountVO.GiftTierVO> tiers = new ArrayList<>();
        for (BalanceProperties.GiftTier tier : properties.giftTiers()) {
            BalanceAccountVO.GiftTierVO item = new BalanceAccountVO.GiftTierVO();
            item.setAmount(tier.getAmount());
            item.setGift(tier.getGift());
            item.setText("充 " + plain(tier.getAmount()) + " 送 " + plain(tier.getGift()));
            tiers.add(item);
        }
        vo.setGiftTiers(tiers);
        return vo;
    }

    public static BalanceRecordVO toRecordVO(BalanceRecord record) {
        BalanceRecordVO vo = new BalanceRecordVO();
        vo.setId(record.getId());
        vo.setMemberId(record.getMemberId());
        vo.setChangeType(record.getChangeType());
        vo.setChangeTypeText(changeTypeText(record.getChangeType()));
        vo.setChangeAmount(amount(record.getChangeAmount()));
        vo.setBalanceAfter(amount(record.getBalanceAfter()));
        vo.setBizType(record.getBizType());
        vo.setBizId(record.getBizId());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    public static BalanceMemberVO toMemberVO(Member member, MemberBalance account) {
        BalanceMemberVO vo = new BalanceMemberVO();
        vo.setMemberId(member.getId());
        vo.setNickname(member.getNickname());
        vo.setPhoneMasked(MaskUtils.maskPhone(member.getPhone()));
        vo.setBalance(amount(account == null ? null : account.getBalance()));
        vo.setTotalRecharge(amount(account == null ? null : account.getTotalRecharge()));
        vo.setTotalGift(amount(account == null ? null : account.getTotalGift()));
        vo.setTotalConsume(amount(account == null ? null : account.getTotalConsume()));
        return vo;
    }

    public static BigDecimal amount(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static String changeTypeText(String changeType) {
        if (changeType == null) {
            return "";
        }
        switch (changeType) {
            case "RECHARGE":
                return "充值";
            case "GIFT":
                return "充值赠送";
            case "CONSUME":
                return "订单消费";
            case "REFUND":
                return "退款退回";
            case "ROLLBACK":
                return "取消退回";
            default:
                return changeType;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
