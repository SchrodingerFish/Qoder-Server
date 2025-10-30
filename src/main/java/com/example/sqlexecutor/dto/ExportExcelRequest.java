package com.example.sqlexecutor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excel导出请求数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportExcelRequest {

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * SQL查询语句
     */
    @NotBlank(message = "SQL查询语句不能为空")
    private String query;

    /**
     * 数据库名称
     */
    @NotBlank(message = "数据库名称不能为空")
    private String database;

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String filename;

    /**
     * 执行选项
     */
    @Valid
    private QueryOptions options;

    /**
     * 查询选项配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryOptions {

        /**
         * 查询超时时间（毫秒）
         */
        private Integer timeout = 30000;

        /**
         * 返回格式
         */
        private String format = "json";

        /**
         * 是否包含元数据
         */
        @JsonProperty("includeMetadata")
        private Boolean includeMetadata = false;

        /**
         * 最大返回行数
         */
        @JsonProperty("maxRows")
        private Integer maxRows = 10000;
    }
}