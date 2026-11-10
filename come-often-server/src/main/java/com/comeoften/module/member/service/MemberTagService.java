package com.comeoften.module.member.service;

import java.util.List;
import java.util.Map;

/**
 * 会员标签读写。写只有沉睡打标这一个入口，读供后台会员列表与沉睡券发放用。
 */
public interface MemberTagService {

    /**
     * 会员 → 标签名集合，批量读，未打标的会员不出现在 Map 里。
     */
    Map<Long, List<String>> tagNamesOf(List<Long> memberIds);

    /**
     * 按标签取会员 ID，最多 limit 条。
     */
    List<Long> memberIdsByTag(String tagCode, int limit);

    /**
     * 按配置阈值刷新沉睡标签：新沉睡的打标、已回访的摘标。
     *
     * @return 打标数 + 摘标数，供任务结果展示
     */
    int refreshSleeping();
}
