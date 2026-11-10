package com.comeoften.module.points.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.points.dto.PointsProfileDTO;
import com.comeoften.module.points.vo.PointsAccountVO;
import com.comeoften.module.points.vo.PointsRecordVO;
import com.comeoften.module.points.vo.PointsRuleVO;

import java.math.BigDecimal;

/**
 * 积分账户与流水。发放一律带业务幂等键，抵现走「下单冻结 → 支付成功实扣 → 取消/超时回滚」，
 * 不做裸 UPDATE 余额式的扣减，计划 3.7。
 */
public interface PointsAccountService {

    /**
     * 账户概览，账户不存在时返回零值账户而非报错。
     */
    PointsAccountVO account(Long memberId);

    /**
     * 本人积分流水分页。
     */
    PageResult<PointsRecordVO> recordPage(Long memberId, Long current, Long size);

    /**
     * 每日签到，每人每天一次。
     */
    PointsAccountVO signIn(Long memberId);

    /**
     * 每日分享，每人每天一次。
     */
    PointsAccountVO share(Long memberId);

    /**
     * 完善资料，终身一次；手机号是登录账号，此处只更新昵称。
     */
    PointsAccountVO completeProfile(Long memberId, PointsProfileDTO dto);

    /**
     * 通用入账，评价（M8）等外部业务复用。返回实际入账积分，重复业务返回 0。
     */
    int earn(Long memberId, String bizType, String bizId, int points, String remark);

    /**
     * 当前订单可用的最大抵扣积分，受余额与券后金额共同约束。
     */
    int maxUsablePoints(Long memberId, BigDecimal afterDiscountAmount);

    /**
     * 解析本次实际使用积分：超余额拒绝，超金额上限按上限收敛。
     */
    int resolveUsePoints(Long memberId, Integer requested, BigDecimal afterDiscountAmount);

    /**
     * 积分抵现金额（元）。
     */
    BigDecimal amountOf(int points);

    /**
     * 当前余额；账户不存在返回 0。
     */
    int balanceOf(Long memberId);

    /**
     * 下单冻结积分，余额不足抛 30033，由调用方事务回滚。
     */
    int freezeForOrder(Long memberId, String orderNo, Integer points);

    /**
     * 支付成功把冻结转为实扣，重复执行无副作用。
     */
    void confirmUseForOrder(Long memberId, String orderNo);

    /**
     * 取消 / 超时回滚冻结积分，金额取自原冻结流水，重复执行无副作用。
     */
    void rollbackForOrder(Long memberId, String orderNo);

    /**
     * 积分兑换菜品所需积分，按金额与兑换比例向上取整。
     */
    int pointsRequired(BigDecimal amount);

    /**
     * 积分兑换下单：直接实扣（无支付环节，不冻结），余额不足抛 30033。
     */
    int redeemForExchange(Long memberId, String orderNo, int points);

    /**
     * 兑换单取消退回积分，幂等。
     */
    void rollbackExchange(Long memberId, String orderNo);

    /**
     * 订单完成发放消费积分，按订单实付金额计算。返回实际发放积分，重复消息返回 0。
     */
    int earnForCompletedOrder(String orderNo);

    /**
     * 每日过期批处理，返回本次过期处理的积分数。
     */
    int expireDue();

    /**
     * 积分规则展示。
     */
    PointsRuleVO rule();
}
