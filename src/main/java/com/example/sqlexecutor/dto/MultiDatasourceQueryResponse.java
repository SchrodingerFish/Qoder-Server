package com.example.sqlexecutor.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多数据源查询响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiDatasourceQueryResponse {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 各数据源查询结果
     */
    private List<DatasourceQueryResult> results;

    /**
     * 总执行时间（毫秒）
     */
    @JsonProperty("totalExecutionTime")
    private long totalExecutionTime;

    /**
     * 单个数据源查询结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatasourceQueryResult {

        /**
         * 数据源编码
         */
        @JsonProperty("datasourceCode")
        private String datasourceCode;

        /**
         * 数据源名称
         */
        @JsonProperty("datasourceName")
        private String datasourceName;

        /**
         * 查询是否成功
         */
        private boolean success;

        /**
         * 查询结果数据
         */
        private List<Map<String, Object>> data;

        /**
         * 返回行数
         */
        @JsonProperty("rowCount")
        private int rowCount;

        /**
         * 受影响行数
         */
        @JsonProperty("rowsAffected")
        private int rowsAffected;

        /**
         * 响应消息
         */
        private String message;

        /**
         * 错误信息（如果有）
         */
        private String error;

        /**
         * 执行时间（毫秒）
         */
        @JsonProperty("executionTime")
        private long executionTime;

        /**
         * 元数据信息
         */
        private ApiResponse.Metadata metadata;
    }
}
