package com.comeoften.module.member.constant;

/**
 * 会员标签编码，取值与 {@code member_tag.tag_code} 的注释一致。
 *
 * <p>M14 B3 只落地沉睡了：阈值 {@code comeoften.member.sleeping-days}（默认 30 天）、
 * 每日 00:30 的 {@code member-tag} 任务负责打标与摘标，后台会员列表可按标签筛选。</p>
 */
public final class MemberTagCode {

    public static final String SLEEPING = "SLEEPING";

    public static final String SLEEPING_NAME = "沉睡会员";

    private MemberTagCode() {
    }
}
