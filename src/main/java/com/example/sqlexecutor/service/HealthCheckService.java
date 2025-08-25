package com.example.sqlexecutor.service;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.time.ZonedDateTime;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.sqlexecutor.dto.HealthResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 健康检查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Value("${spring.application.name:sql-executor}")
    private String applicationName;

    @Value("${application.version:1.0.0}")
    private String applicationVersion;

    // 应用启动时间
    private final ZonedDateTime startTime = ZonedDateTime.now();

    /**
     * 获取基本健康状态
     */
    public HealthResponse getBasicHealth() {
        return HealthResponse.ok();
    }

    /**
     * 获取详细健康状态
     */
    public HealthResponse getDetailedHealth() {
        try {
            HealthResponse.DatabaseStatus databaseStatus = getDatabaseStatus();
            HealthResponse.AppInfo appInfo = getAppInfo();

            return HealthResponse.detailed(databaseStatus, appInfo);

        } catch (Exception e) {
            log.error("获取详细健康状态失败", e);
            throw new RuntimeException("健康检查失败", e);
        }
    }

    /**
     * 获取数据库状态
     */
    public HealthResponse.DatabaseStatus getDatabaseStatus() {
        try {
            long startTime = System.currentTimeMillis();

            // 执行简单的数据库查询来测试连接
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            long responseTime = System.currentTimeMillis() - startTime;

            // 获取连接池信息（HikariCP）
            HealthResponse.ConnectionPoolInfo poolInfo = getConnectionPoolInfo();

            return HealthResponse.DatabaseStatus.builder()
                    .status("ok")
                    .connectionPool(poolInfo)
                    .responseTime(responseTime)
                    .build();

        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return HealthResponse.DatabaseStatus.builder()
                    .status("error")
                    .responseTime(-1L)
                    .build();
        }
    }

    /**
     * 获取连接池信息
     */
    private HealthResponse.ConnectionPoolInfo getConnectionPoolInfo() {
        try {
            // 如果使用HikariCP，可以通过以下方式获取连接池信息
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;

                com.zaxxer.hikari.HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

                return HealthResponse.ConnectionPoolInfo.builder()
                        .active(poolBean.getActiveConnections())
                        .idle(poolBean.getIdleConnections())
                        .max(hikariDataSource.getMaximumPoolSize())
                        .min(hikariDataSource.getMinimumIdle())
                        .build();
            }

            // 默认返回基本信息
            return HealthResponse.ConnectionPoolInfo.builder()
                    .active(0)
                    .idle(0)
                    .max(20)
                    .min(5)
                    .build();

        } catch (Exception e) {
            log.warn("获取连接池信息失败", e);
            return HealthResponse.ConnectionPoolInfo.builder()
                    .active(-1)
                    .idle(-1)
                    .max(-1)
                    .min(-1)
                    .build();
        }
    }

    /**
     * 获取应用信息
     */
    private HealthResponse.AppInfo getAppInfo() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        return HealthResponse.AppInfo.builder()
                .name(applicationName)
                .version(applicationVersion)
                .startTime(startTime)
                .uptime(uptime)
                .build();
    }

    /**
     * 测试数据库连接
     */
    public boolean testDatabaseConnection() {
        try {
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(5); // 5秒超时
            connection.close();
            return isValid;

        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            return false;
        }
    }
}