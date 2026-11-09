package com.comeoften.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体。
 */
@Data
@Schema(description = "分页响应体")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页，从 1 开始", example = "1")
    private Long current;

    @Schema(description = "每页条数", example = "10")
    private Long size;

    @Schema(description = "当前页记录")
    private List<T> records;

    public static <T> PageResult<T> of(Long total, Long current, Long size, List<T> records) {
        PageResult<T> page = new PageResult<>();
        page.setTotal(total);
        page.setCurrent(current);
        page.setSize(size);
        page.setRecords(records);
        return page;
    }
}
