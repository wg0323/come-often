package com.comeoften.module.home.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.dish.dto.DishQueryDTO;
import com.comeoften.module.dish.service.DishQueryService;
import com.comeoften.module.home.converter.HomeConverter;
import com.comeoften.module.home.dto.BannerSaveDTO;
import com.comeoften.module.home.dto.NoticeSaveDTO;
import com.comeoften.module.home.entity.Banner;
import com.comeoften.module.home.entity.Notice;
import com.comeoften.module.home.mapper.BannerMapper;
import com.comeoften.module.home.mapper.NoticeMapper;
import com.comeoften.module.home.service.HomeService;
import com.comeoften.module.home.vo.BannerVO;
import com.comeoften.module.home.vo.HomeSummaryVO;
import com.comeoften.module.home.vo.NoticeVO;
import com.comeoften.module.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 首页汇总与运营位。轮播最多 3 张，按权重降序；公告与轮播只返回当前生效记录。
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final long BANNER_LIMIT = 3L;

    private final StoreService storeService;

    private final DishQueryService dishQueryService;

    private final BannerMapper bannerMapper;

    private final NoticeMapper noticeMapper;

    @Override
    public HomeSummaryVO summary(Long storeId) {
        HomeSummaryVO vo = new HomeSummaryVO();
        vo.setStore(storeService.detail(storeId));
        vo.setBanners(listBanners(storeId).stream().limit(BANNER_LIMIT).collect(Collectors.toList()));
        vo.setNotices(listNotices(storeId));
        DishQueryDTO query = new DishQueryDTO();
        query.setStoreId(storeId);
        query.setSignature(Boolean.TRUE);
        vo.setSignatureDishes(dishQueryService.list(query));
        return vo;
    }

    @Override
    public List<BannerVO> listBanners(Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStoreId, storeId)
                        .and(wrapper -> wrapper.isNull(Banner::getStartTime)
                                .or().le(Banner::getStartTime, now))
                        .and(wrapper -> wrapper.isNull(Banner::getEndTime)
                                .or().ge(Banner::getEndTime, now))
                        .orderByDesc(Banner::getWeight)
                        .orderByAsc(Banner::getId))
                .stream().map(HomeConverter::toBannerVO).collect(Collectors.toList());
    }

    @Override
    public Long saveBanner(BannerSaveDTO dto, Long storeId) {
        if (dto.getId() != null) {
            requireBanner(dto.getId(), storeId);
        }
        Banner banner = new Banner();
        BeanUtils.copyProperties(dto, banner);
        banner.setStoreId(storeId);
        banner.setJumpType(StringUtils.hasText(dto.getJumpType()) ? dto.getJumpType() : "NONE");
        banner.setJumpValue(dto.getJumpValue() == null ? "" : dto.getJumpValue());
        banner.setWeight(dto.getWeight() == null ? 0 : dto.getWeight());
        banner.setStartTime(parseTime(dto.getStartTime()));
        banner.setEndTime(parseTime(dto.getEndTime()));
        if (dto.getId() == null) {
            bannerMapper.insert(banner);
        } else {
            bannerMapper.updateById(banner);
        }
        return banner.getId();
    }

    @Override
    public void removeBanner(Long bannerId, Long storeId) {
        requireBanner(bannerId, storeId);
        bannerMapper.deleteById(bannerId);
    }

    @Override
    public List<NoticeVO> listNotices(Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        return noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .eq(Notice::getStoreId, storeId)
                        .and(wrapper -> wrapper.isNull(Notice::getStartTime)
                                .or().le(Notice::getStartTime, now))
                        .and(wrapper -> wrapper.isNull(Notice::getEndTime)
                                .or().ge(Notice::getEndTime, now))
                        .orderByAsc(Notice::getId))
                .stream().map(HomeConverter::toNoticeVO).collect(Collectors.toList());
    }

    @Override
    public Long saveNotice(NoticeSaveDTO dto, Long storeId) {
        if (dto.getId() != null) {
            requireNotice(dto.getId(), storeId);
        }
        Notice notice = new Notice();
        BeanUtils.copyProperties(dto, notice);
        notice.setStoreId(storeId);
        notice.setNoticeType(StringUtils.hasText(dto.getNoticeType()) ? dto.getNoticeType() : "NORMAL");
        notice.setStartTime(parseTime(dto.getStartTime()));
        notice.setEndTime(parseTime(dto.getEndTime()));
        if (dto.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        return notice.getId();
    }

    @Override
    public void removeNotice(Long noticeId, Long storeId) {
        requireNotice(noticeId, storeId);
        noticeMapper.deleteById(noticeId);
    }

    private void requireBanner(Long bannerId, Long storeId) {
        Banner banner = bannerMapper.selectById(bannerId);
        if (banner == null || !Objects.equals(banner.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "轮播不存在");
        }
    }

    private void requireNotice(Long noticeId, Long storeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null || !Objects.equals(notice.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "公告不存在");
        }
    }

    private static LocalDateTime parseTime(String value) {
        return StringUtils.hasText(value) ? LocalDateTime.parse(value, TIME_FORMATTER) : null;
    }
}
