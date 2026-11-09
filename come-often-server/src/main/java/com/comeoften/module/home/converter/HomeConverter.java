package com.comeoften.module.home.converter;

import com.comeoften.module.home.entity.Banner;
import com.comeoften.module.home.entity.Notice;
import com.comeoften.module.home.vo.BannerVO;
import com.comeoften.module.home.vo.HomeSummaryVO;
import com.comeoften.module.home.vo.NoticeVO;
import org.springframework.beans.BeanUtils;

/**
 * 运营位对象转换。
 */
public final class HomeConverter {

    private HomeConverter() {
    }

    public static BannerVO toBannerVO(Banner banner) {
        BannerVO vo = new BannerVO();
        BeanUtils.copyProperties(banner, vo);
        return vo;
    }

    public static NoticeVO toNoticeVO(Notice notice) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        return vo;
    }

    public static HomeSummaryVO.SectionVO toSectionVO(String key, String title, String theme,
                                                       Long categoryId, long dishCount) {
        HomeSummaryVO.SectionVO vo = new HomeSummaryVO.SectionVO();
        vo.setKey(key);
        vo.setTitle(title);
        vo.setTheme(theme);
        vo.setCategoryId(categoryId);
        vo.setDishCount(dishCount);
        vo.setDesc(dishCount + " 道");
        return vo;
    }
}
