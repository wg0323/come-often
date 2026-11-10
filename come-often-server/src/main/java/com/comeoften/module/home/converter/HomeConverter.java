package com.comeoften.module.home.converter;

import com.comeoften.module.home.entity.Banner;
import com.comeoften.module.home.entity.Notice;
import com.comeoften.module.home.vo.BannerVO;
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
}
