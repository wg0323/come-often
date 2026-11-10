package com.comeoften.module.bundle.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.bundle.dto.BundleSaveDTO;
import com.comeoften.module.bundle.entity.GroupBundle;
import com.comeoften.module.bundle.mapper.GroupBundleItemMapper;
import com.comeoften.module.bundle.mapper.GroupBundleMapper;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.mapper.DishMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 套餐库存：编辑总库存时已售份数不变、售罄拒绝下单、明细菜品必须属于本店。
 */
@ExtendWith(MockitoExtension.class)
class GroupBundleServiceImplTest {

    private static final Long STORE_ID = 1L;

    private static final Long BUNDLE_ID = 99L;

    private static final Long DISH_ID = 1001L;

    @Mock
    private GroupBundleMapper groupBundleMapper;

    @Mock
    private GroupBundleItemMapper groupBundleItemMapper;

    @Mock
    private DishMapper dishMapper;

    @InjectMocks
    private GroupBundleServiceImpl groupBundleService;

    @Test
    void should_keep_sold_quantity_when_total_stock_changes() {
        GroupBundle current = bundle(50, 30, 1);
        when(groupBundleMapper.selectById(BUNDLE_ID)).thenReturn(current);
        Dish dish = new Dish();
        dish.setId(DISH_ID);
        dish.setStoreId(STORE_ID);
        dish.setDishName("九转大肠");
        when(dishMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(dish));

        groupBundleService.save(STORE_ID, saveDTO(80));

        ArgumentCaptor<GroupBundle> captor = ArgumentCaptor.forClass(GroupBundle.class);
        verify(groupBundleMapper).updateById(captor.capture());
        // 原已售 20 份：新总库存 80 时余量应为 60，不能凭空补货
        assertThat(captor.getValue().getRemainStock()).isEqualTo(60);
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    @Test
    void should_reject_item_from_other_store() {
        GroupBundle current = bundle(50, 50, 1);
        when(groupBundleMapper.selectById(BUNDLE_ID)).thenReturn(current);
        Dish dish = new Dish();
        dish.setId(DISH_ID);
        dish.setStoreId(999L);
        when(dishMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(dish));

        BizException ex = catchThrowableOfType(
                () -> groupBundleService.save(STORE_ID, saveDTO(60)), BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.GROUP_BUNDLE_ITEM_INVALID);
        verify(groupBundleMapper, never()).updateById(any(GroupBundle.class));
    }

    @Test
    void should_reject_purchase_when_bundle_is_sold_out() {
        when(groupBundleMapper.selectById(BUNDLE_ID)).thenReturn(bundle(50, 0, 1));

        BizException ex = catchThrowableOfType(
                () -> groupBundleService.requireAndDeduct(STORE_ID, BUNDLE_ID, 1), BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.GROUP_BUNDLE_SOLD_OUT);
        verify(groupBundleMapper, never()).deductStock(BUNDLE_ID, 1);
    }

    private static GroupBundle bundle(int total, int remain, int status) {
        GroupBundle bundle = new GroupBundle();
        bundle.setId(BUNDLE_ID);
        bundle.setStoreId(STORE_ID);
        bundle.setTotalStock(total);
        bundle.setRemainStock(remain);
        bundle.setStatus(status);
        return bundle;
    }

    private static BundleSaveDTO saveDTO(int totalStock) {
        BundleSaveDTO dto = new BundleSaveDTO();
        dto.setId(BUNDLE_ID);
        dto.setBundleName("双人尝鲜套餐");
        dto.setBundlePrice(new BigDecimal("88.00"));
        dto.setTotalStock(totalStock);
        BundleSaveDTO.ItemDTO item = new BundleSaveDTO.ItemDTO();
        item.setDishId(DISH_ID);
        item.setQuantity(1);
        dto.setItems(Collections.singletonList(item));
        return dto;
    }
}
