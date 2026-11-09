package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.module.dish.converter.DishListAssembler;
import com.comeoften.module.dish.dto.DishSearchDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishTag;
import com.comeoften.module.dish.entity.DishTagRel;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishTagMapper;
import com.comeoften.module.dish.mapper.DishTagRelMapper;
import com.comeoften.module.dish.service.DishSearchService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.util.DishSearchRanker;
import com.comeoften.module.dish.vo.DishListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 程序内搜索实现。
 *
 * <p>SQL 只做候选集：菜名 / 副标题 / 描述右模糊 + 标签名右模糊关联，全部限定本店上架菜品，
 * 不出现 <code>LIKE '%kw%'</code>。排序（名称 &gt; 标签 &gt; 副标题 &gt; 描述，同档招牌优先）
 * 由 {@link DishSearchRanker} 完成，候选集就是本店上架菜，量级可控。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishSearchServiceImpl implements DishSearchService {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 20L;

    private static final int MAX_KEYWORD_LENGTH = 30;

    private final DishMapper dishMapper;

    private final DishTagMapper dishTagMapper;

    private final DishTagRelMapper dishTagRelMapper;

    private final DishListAssembler dishListAssembler;

    @Override
    public PageResult<DishListVO> search(DishSearchDTO query) {
        long pageNo = query.getCurrent() == null || query.getCurrent() < 1 ? 1L : query.getCurrent();
        long pageSize = query.getSize() == null || query.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(query.getSize(), MAX_PAGE_SIZE);
        String keyword = DishSearchRanker.normalize(query.getKeyword());
        if (!StringUtils.hasText(keyword)) {
            // 空关键词不返回全量：搜索页在未输入时应展示提示，而不是把菜单倒一遍
            return PageResult.of(0L, pageNo, pageSize, Collections.emptyList());
        }
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID, "搜索关键词最多 " + MAX_KEYWORD_LENGTH + " 个字");
        }
        List<Dish> candidates = loadCandidates(query.getStoreId(), keyword);
        if (candidates.isEmpty()) {
            return PageResult.of(0L, pageNo, pageSize, Collections.emptyList());
        }
        Map<Long, List<String>> tagNames = loadTagNames(candidates);
        Set<Long> signatureDishIds = loadSignatureDishIds();
        List<Dish> sorted = DishSearchRanker.sort(keyword, candidates, tagNames::get, signatureDishIds);
        // 先用 long 算起点：超大页码不溢出成负数，直接返回空页，与 DB 分页行为一致
        long startIndex = (pageNo - 1L) * pageSize;
        if (startIndex >= sorted.size()) {
            return PageResult.of((long) sorted.size(), pageNo, pageSize, Collections.emptyList());
        }
        int fromIndex = (int) startIndex;
        int toIndex = (int) Math.min(startIndex + pageSize, sorted.size());
        List<Dish> pageDishes = sorted.subList(fromIndex, toIndex);
        List<DishListVO> records = dishListAssembler.assemble(query.getStoreId(), pageDishes);
        log.debug("菜品搜索完成: keyword={}, candidates={}, total={}, page={}",
                keyword, candidates.size(), sorted.size(), pageNo);
        return PageResult.of((long) sorted.size(), pageNo, pageSize, records);
    }

    // ------------------------------------------------------------------ 私有逻辑

    private List<Dish> loadCandidates(Long storeId, String keyword) {
        // 绑定参数不会有注入问题，但 % 与 _ 仍是 LIKE 通配符：不转义时输入 "%" 等价于全模糊
        String likeValue = escapeLike(keyword);
        List<Long> tagMatchedDishIds = findTagMatchedDishIds(likeValue);
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, storeId)
                .eq(Dish::getStatus, DishConstants.STATUS_ON_SHELF)
                .and(inner -> {
                    inner.likeRight(Dish::getDishName, likeValue)
                            .or().likeRight(Dish::getSubtitle, likeValue)
                            .or().likeRight(Dish::getDescription, likeValue);
                    if (!tagMatchedDishIds.isEmpty()) {
                        inner.or().in(Dish::getId, tagMatchedDishIds);
                    }
                });
        return dishMapper.selectList(wrapper);
    }

    /**
     * 转义 LIKE 通配符，保证关键词只作为字面前缀参与匹配。
     */
    private static String escapeLike(String keyword) {
        return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * 标签名右模糊命中的菜品 ID；标签与菜品是多对多，只取关联表里的 dish_id。
     */
    private List<Long> findTagMatchedDishIds(String keyword) {
        List<Long> tagIds = dishTagMapper.selectList(new LambdaQueryWrapper<DishTag>()
                        .likeRight(DishTag::getTagName, keyword))
                .stream().map(DishTag::getId).collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                        .in(DishTagRel::getTagId, tagIds))
                .stream().map(DishTagRel::getDishId).distinct().collect(Collectors.toList());
    }

    private Map<Long, List<String>> loadTagNames(List<Dish> dishes) {
        List<Long> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());
        List<DishTagRel> rels = dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                .in(DishTagRel::getDishId, dishIds));
        if (rels.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = rels.stream().map(DishTagRel::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> names = dishTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(DishTag::getId, DishTag::getTagName, (left, right) -> left));
        Map<Long, List<String>> result = new HashMap<>();
        for (DishTagRel rel : rels) {
            String name = names.get(rel.getTagId());
            if (name != null) {
                result.computeIfAbsent(rel.getDishId(), key -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    private Set<Long> loadSignatureDishIds() {
        DishTag tag = dishTagMapper.selectOne(new LambdaQueryWrapper<DishTag>()
                .eq(DishTag::getTagCode, DishConstants.SIGNATURE_TAG_CODE));
        if (tag == null) {
            return Collections.emptySet();
        }
        Collection<DishTagRel> rels = dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                .eq(DishTagRel::getTagId, tag.getId()));
        return rels.stream().map(DishTagRel::getDishId).collect(Collectors.toCollection(HashSet::new));
    }
}
