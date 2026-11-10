package com.comeoften.module.bundle.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.bundle.dto.BundleSaveDTO;
import com.comeoften.module.bundle.vo.BundleVO;

import java.util.List;

/**
 * 团购套餐。顾客端只看到上架且有余量的套餐；下单库存扣减与取消回补都在订单事务内调用。
 */
public interface GroupBundleService {

    /**
     * 小程序专属列表：上架且剩余库存大于 0。
     */
    List<BundleVO> listOnShelf(Long storeId);

    /**
     * 套餐详情；requireOnShelf 为 true 时下架或售罄直接抛业务码。
     */
    BundleVO detail(Long storeId, Long bundleId, boolean requireOnShelf);

    PageResult<BundleVO> pageForAdmin(Long storeId, Long current, Long size,
                                      Integer status, String keyword);

    /**
     * 新增或整体覆盖式编辑，返回套餐 ID。
     */
    Long save(Long storeId, BundleSaveDTO dto);

    void updateStatus(Long storeId, Long bundleId, Integer status);

    void remove(Long storeId, Long bundleId);

    /**
     * 下单前置：校验归属 / 上架 / 余量并 CAS 扣减库存，返回含明细的套餐快照。
     * 必须与订单插入在同一事务里，失败时由事务回滚 DB 更新。
     */
    BundleVO requireAndDeduct(Long storeId, Long bundleId, int quantity);

    /**
     * 订单取消 / 超时回补库存。
     */
    void restoreStock(Long bundleId, int quantity);
}
