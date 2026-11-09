package com.comeoften.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.module.payment.entity.RefundRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 退款单映射。退款单表本身不带 store_id，后台分页用两表 join 限定门店。
 */
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    String COLUMNS = "r.id, r.refund_no, r.order_id, r.payment_id, r.refund_amount, r.reason, "
            + "r.status, r.audit_user_id, r.refund_time, r.is_deleted, r.create_time, r.update_time";

    @Select("<script>SELECT " + COLUMNS + " FROM refund_record r "
            + "INNER JOIN trade_order o ON o.id = r.order_id "
            + "WHERE o.store_id = #{storeId} AND r.is_deleted = 0 "
            + "<if test='status != null'>AND r.status = #{status}</if> "
            + "ORDER BY r.create_time DESC</script>")
    IPage<RefundRecord> pageByStore(Page<?> page, @Param("storeId") Long storeId,
                                    @Param("status") Integer status);
}
