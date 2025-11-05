package com.example.sqlexecutor.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API响应数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 返回的数据
     */
    private T data;

    /**
     * 返回的行数
     */
    @JsonProperty("rowCount")
    private int rowCount;

    /**
     * 受影响的行数
     */
    @JsonProperty("rowsAffected")
    private int rowsAffected;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 执行时间（毫秒）
     */
    @JsonProperty("executionTime")
    private long executionTime;

    /**
     * 元数据信息（可选）
     */
    private Metadata metadata;

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data, int rowCount, int rowsAffected,
            String message, long executionTime) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .rowCount(rowCount)
                .rowsAffected(rowsAffected)
                .message(message)
                .executionTime(executionTime)
                .build();
    }

    /**
     * 创建成功响应（带元数据）
     */
    public static <T> ApiResponse<T> success(T data, int rowCount, int rowsAffected,
            String message, long executionTime, Metadata metadata) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .rowCount(rowCount)
                .rowsAffected(rowsAffected)
                .message(message)
                .executionTime(executionTime)
                .metadata(metadata)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(null)
                .rowCount(0)
                .rowsAffected(0)
                .message(message)
                .executionTime(0)
                .build();
    }

    /**
     * 元数据信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metadata {

        /**
         * 列信息
         */
        private List<ColumnInfo> columns;

        /**
         * 查询类型
         */
        private String queryType;

        /**
         * 数据库信息
         */
        private String database;

        /**
         * 执行计划（可选）
         */
        private Map<String, Object> executionPlan;
    }

    /**
     * 列信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnInfo {

        /**
         * 列名
         */
        private String name;

        /**
         * 数据类型
         */
        private String type;

        /**
         * 是否可为空
         */
        private boolean nullable;

        /**
         * 列长度
         */
        private Integer length;

        /**
         * 精度
         */
        private Integer precision;

        /**
         * 小数位数
         */
        private Integer scale;
    }
}