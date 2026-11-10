package com.comeoften.module.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员标签。{@code uk_member_tag(member_id, tag_code)} 不含 is_deleted，
 * 因此「摘标再打标」要用原生 UPDATE 复活而不是直接 insert（见 deploy/init.md 差异 59 / 631）。
 */
@Data
@TableName("member_tag")
public class MemberTag implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long memberId;

    private String tagCode;

    private String tagName;

    /** 标签失效时间；为空表示长期有效，由任务在会员回访时摘标。 */
    private LocalDateTime expireTime;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
