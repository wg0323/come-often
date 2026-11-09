package com.comeoften.module.home.vo;

import com.comeoften.module.dish.vo.DishListVO;
import com.comeoften.module.store.vo.StoreVO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 首页汇总。一次请求返回门店条、轮播、公告、两个分层入口与招牌菜横滑，避免首屏多次往返。
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

    @Schema(description = "分层入口：家常小炒与需提前预定")
    private List<SectionVO> sections;

    @Schema(description = "招牌菜横滑")
    private List<DishListVO> signatureDishes;

    /**
     * 首页分层入口。
     */
    @Data
    @Schema(description = "首页分层入口")
    public static class SectionVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "入口标识：daily / preorder")
        private String key;

        @Schema(description = "入口标题")
        private String title;

        @Schema(description = "菜品数描述", example = "14 道")
        private String desc;

        @Schema(description = "视觉主题：ink / gold")
        private String theme;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "点击后跳转的分类 ID；preorder 入口指向预定分类")
        private Long categoryId;

        @Schema(description = "该入口菜品数")
        private Long dishCount;
    }
}
