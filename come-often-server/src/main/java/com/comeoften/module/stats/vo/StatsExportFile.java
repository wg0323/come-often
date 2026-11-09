package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 内部载体：导出文件内容与文件名。不直接出接口，由 Controller 写到响应流。
 */
@Data
public class StatsExportFile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;

    private byte[] content;

    public static StatsExportFile of(String fileName, byte[] content) {
        StatsExportFile file = new StatsExportFile();
        file.setFileName(fileName);
        file.setContent(content);
        return file;
    }
}
