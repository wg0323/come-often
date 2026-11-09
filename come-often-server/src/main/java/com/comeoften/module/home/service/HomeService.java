package com.comeoften.module.home.service;

import com.comeoften.module.home.dto.BannerSaveDTO;
import com.comeoften.module.home.dto.NoticeSaveDTO;
import com.comeoften.module.home.vo.BannerVO;
import com.comeoften.module.home.vo.HomeSummaryVO;
import com.comeoften.module.home.vo.NoticeVO;

import java.util.List;

/**
 * 首页汇总与运营位管理。轮播按权重排序，且只返回当前生效的记录。
 * 所有写操作显式带 storeId，按 ID 修改/删除前先校验记录归属。
 */
public interface HomeService {

    HomeSummaryVO summary(Long storeId);

    List<BannerVO> listBanners(Long storeId);

    Long saveBanner(BannerSaveDTO dto, Long storeId);

    void removeBanner(Long bannerId, Long storeId);

    List<NoticeVO> listNotices(Long storeId);

    Long saveNotice(NoticeSaveDTO dto, Long storeId);

    void removeNotice(Long noticeId, Long storeId);
}
