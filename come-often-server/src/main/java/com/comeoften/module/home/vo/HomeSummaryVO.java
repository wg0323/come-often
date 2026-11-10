package com.comeoften.module.home.vo;

import com.comeoften.module.dish.vo.DishListVO;
import com.comeoften.module.store.vo.StoreVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 首页汇总。一次请求返回门店条、轮播、公告与招牌菜横滑，避免首屏多次往返。
 */
@Data
@Schema(description = "首页汇总")
public class HomeSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店条")
    private StoreVO store;

    @Schema(description = "轮播，最多 3 张，含 1 张预定菜")
    private List<BannerVO> banners;

    @Schema(description = "公告，单行滚动")
    private List<NoticeVO> notices;

    @Schema(description = "招牌菜横滑")
    private List<DishListVO> signatureDishes;
}
