package com.comeoften.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.coupon.entity.CouponUseRecord;

/**
 * 券核销记录 Mapper。uk_user_coupon 唯一索引兜住并发重复写入。
 */
public interface CouponUseRecordMapper extends BaseMapper<CouponUseRecord> {
}
