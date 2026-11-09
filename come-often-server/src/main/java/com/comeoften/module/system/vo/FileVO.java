package com.comeoften.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果。
 */
@Data
@Schema(description = "文件上传结果")
public class FileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "对象键，数据库只存该值", example = "dish/8f2c4a10b6d4111ee9abc0242ac120002_list.webp")
    private String objectKey;

    @Schema(description = "文件大小，单位字节", example = "78642")
    private Long size;
}
