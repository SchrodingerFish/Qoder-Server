package com.example.sqlexecutor.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多数据源Excel导出请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiDatasourceExportRequest {

    /**
     * SQL查询语句
     */
    @NotNull(message = "查询语句不能为空")
    private String query;

    /**
     * 数据源编码列表
     */
    @NotEmpty(message = "数据源列表不能为空")
    private List<String> datasourceCodes;

    /**
     * 导出文件名前缀（不包含扩展名）
     */
    private String filenamePrefix;

    /**
     * 查询选项
     */
    private QueryOptions options;

    /**
     * 查询选项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueryOptions {

        /**
         * 超时时间（毫秒）
         */
        private Integer timeout;

        /**
         * 最大返回行数
         */
        private Integer maxRows;

        /**
         * 是否包含元数据
         */
        private Boolean includeMetadata;

        /**
         * 返回格式
         */
        private String format;
    }
}
