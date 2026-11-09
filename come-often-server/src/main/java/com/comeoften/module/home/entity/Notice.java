package com.comeoften.module.home.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页公告，单行滚动，不做弹窗。
 */
@Data
@TableName("notice")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private String content;
    private String noticeType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
