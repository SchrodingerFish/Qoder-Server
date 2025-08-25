package com.example.sqlexecutor.dto;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查响应数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {

    /**
     * 服务状态
     */
    private String status;

    /**
     * 时间戳
     */
    private ZonedDateTime timestamp;

    /**
     * 数据库连接状态（可选）
     */
    private DatabaseStatus database;

    /**
     * 应用信息（可选）
     */
    private AppInfo appInfo;

    /**
     * 创建健康响应
     */
    public static HealthResponse ok() {
        return HealthResponse.builder()
                .status("ok")
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * 创建详细健康响应
     */
    public static HealthResponse detailed(DatabaseStatus database, AppInfo appInfo) {
        return HealthResponse.builder()
                .status("ok")
                .timestamp(ZonedDateTime.now())
                .database(database)
                .appInfo(appInfo)
                .build();
    }

    /**
     * 数据库状态
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatabaseStatus {

        /**
         * 数据库连接状态
         */
        private String status;

        /**
         * 连接池信息
         */
        private ConnectionPoolInfo connectionPool;

        /**
         * 响应时间（毫秒）
         */
        private Long responseTime;
    }

    /**
     * 连接池信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectionPoolInfo {

        /**
         * 活跃连接数
         */
        private int active;

        /**
         * 空闲连接数
         */
        private int idle;

        /**
         * 最大连接数
         */
        private int max;

        /**
         * 最小连接数
         */
        private int min;
    }

    /**
     * 应用信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppInfo {

        /**
         * 应用名称
         */
        private String name;

        /**
         * 应用版本
         */
        private String version;

        /**
         * 启动时间
         */
        private ZonedDateTime startTime;

        /**
         * 运行时间（毫秒）
         */
        private long uptime;
    }
}