package com.comeoften.module.bundle.converter;

import com.comeoften.module.bundle.constant.BundleStatus;
import com.comeoften.module.bundle.entity.GroupBundle;
import com.comeoften.module.bundle.entity.GroupBundleItem;
import com.comeoften.module.bundle.vo.BundleItemVO;
import com.comeoften.module.bundle.vo.BundleVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐实体转展示对象。列表与详情共用，避免两端各拼一份字段。
 */
public final class BundleConverter {

    private BundleConverter() {
    }

    public static BundleVO toVO(GroupBundle bundle, List<GroupBundleItem> items) {
        BundleVO vo = new BundleVO();
        vo.setId(bundle.getId());
        vo.setBundleName(bundle.getBundleName());
        vo.setDescription(bundle.getDescription());
        vo.setBundlePrice(bundle.getBundlePrice());
        vo.setOriginalPrice(bundle.getOriginalPrice());
        vo.setTotalStock(bundle.getTotalStock());
        vo.setRemainStock(bundle.getRemainStock());
        vo.setStatus(bundle.getStatus());
        BundleStatus status = BundleStatus.of(bundle.getStatus());
        vo.setStatusText(status == null ? "" : status.getDescription());
        vo.setCreateTime(bundle.getCreateTime());
        vo.setItems(items == null || items.isEmpty() ? Collections.emptyList()
                : items.stream().map(BundleConverter::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    public static BundleItemVO toItemVO(GroupBundleItem item) {
        BundleItemVO vo = new BundleItemVO();
        vo.setDishId(item.getDishId());
        vo.setDishName(item.getDishName());
        vo.setQuantity(item.getQuantity());
        vo.setSort(item.getSort());
        return vo;
    }
}
