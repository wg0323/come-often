package com.comeoften.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.order.entity.PaymentRecord;

/**
 * 支付流水 Mapper，out_trade_no 唯一索引保证回调幂等。
 */
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
