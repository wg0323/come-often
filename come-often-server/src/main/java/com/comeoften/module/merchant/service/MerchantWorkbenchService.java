package com.comeoften.module.merchant.service;

import com.comeoften.module.merchant.vo.WorkbenchSummaryVO;

/**
 * 商家端工作台概览。三个订单数字与订单看板同源，页面不做任何业务计算。
 */
public interface MerchantWorkbenchService {

    WorkbenchSummaryVO summary(Long storeId);
}
