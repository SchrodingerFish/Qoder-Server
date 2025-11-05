package com.example.sqlexecutor.service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.example.sqlexecutor.dto.ExecuteSqlRequest;
import com.example.sqlexecutor.dto.MultiDatasourceQueryRequest;
import com.example.sqlexecutor.dto.MultiDatasourceQueryResponse;
import com.example.sqlexecutor.entity.DatasourceConfig;
import com.example.sqlexecutor.exception.InvalidSqlException;
import com.example.sqlexecutor.util.PasswordEncryptor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 多数据源并行查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiDatasourceQueryService {

    private final DatasourceService datasourceService;
    private final SqlExecutionService sqlExecutionService;
    private final PasswordEncryptor passwordEncryptor;

    @Value("${sql-executor.multi-datasource.thread-pool-size:10}")
    private int threadPoolSize;

    @Value("${sql-executor.default-timeout:30000}")
    private int defaultTimeout;

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(threadPoolSize);
        log.info("多数据源查询服务初始化，线程池大小: {}", threadPoolSize);
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
            log.info("多数据源查询服务线程池已关闭");
        }
    }

    /**
     * 并行执行多数据源查询
     */
    public MultiDatasourceQueryResponse executeMultiDatasourceQuery(MultiDatasourceQueryRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("开始执行多数据源并行查询，数据源数量: {}", request.getDatasourceCodes().size());

        try {
            // 验证SQL
            sqlExecutionService.validateSql(request.getQuery());

            // 批量获取数据源配置
            Map<String, DatasourceConfig> datasourceConfigs = datasourceService
                    .getDatasourcesByCodes(request.getDatasourceCodes());

            // 检查是否所有数据源都存在
            List<String> missingDatasources = request.getDatasourceCodes().stream()
                    .filter(code -> !datasourceConfigs.containsKey(code))
                    .collect(Collectors.toList());

            if (!missingDatasources.isEmpty()) {
                return MultiDatasourceQueryResponse.builder()
                        .success(false)
                        .message("以下数据源不存在或已禁用: " + String.join(", ", missingDatasources))
                        .results(new ArrayList<>())
                        .totalExecutionTime(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 并行执行查询
            List<CompletableFuture<MultiDatasourceQueryResponse.DatasourceQueryResult>> futures = request
                    .getDatasourceCodes().stream()
                    .map(datasourceCode -> CompletableFuture.supplyAsync(() -> {
                        DatasourceConfig config = datasourceConfigs.get(datasourceCode);
                        return executeSingleDatasourceQuery(config, request.getQuery(), request.getOptions());
                    }, executorService))
                    .collect(Collectors.toList());

            // 等待所有查询完成
            CompletableFuture<Void> allFutures = CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]));

            allFutures.join();

            // 收集结果
            List<MultiDatasourceQueryResponse.DatasourceQueryResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            long totalExecutionTime = System.currentTimeMillis() - startTime;

            // 检查是否有失败的查询
            long failedCount = results.stream().filter(r -> !r.isSuccess()).count();
            long successCount = results.stream().filter(MultiDatasourceQueryResponse.DatasourceQueryResult::isSuccess)
                    .count();

            String message = String.format("多数据源查询完成: 成功 %d/%d, 失败 %d/%d",
                    successCount, results.size(), failedCount, results.size());

            log.info("{}, 总执行时间: {}ms", message, totalExecutionTime);

            // 即使部分查询失败，也返回成功状态，让前端能够显示所有结果
            return MultiDatasourceQueryResponse.builder()
                    .success(true) // 总是返回true，以便前端可以显示所有结果标签页
                    .message(message)
                    .results(results)
                    .totalExecutionTime(totalExecutionTime)
                    .build();

        } catch (InvalidSqlException e) {
            log.warn("SQL验证失败: {}", e.getMessage());
            return MultiDatasourceQueryResponse.builder()
                    .success(false)
                    .message("SQL验证失败: " + e.getMessage())
                    .results(new ArrayList<>())
                    .totalExecutionTime(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("多数据源查询执行异常", e);
            return MultiDatasourceQueryResponse.builder()
                    .success(false)
                    .message("多数据源查询执行失败: " + e.getMessage())
                    .results(new ArrayList<>())
                    .totalExecutionTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 执行单个数据源查询
     */
    private MultiDatasourceQueryResponse.DatasourceQueryResult executeSingleDatasourceQuery(
            DatasourceConfig config, String query, ExecuteSqlRequest.QueryOptions options) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("开始查询数据源: {} [{}]", config.getDatasourceName(), config.getDatasourceCode());

            // 创建数据源连接
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName(config.getDriverClass());
            dataSource.setUrl(config.getJdbcUrl());
            dataSource.setUsername(config.getUsername());

            // 解密密码
            String password = config.getPassword();
            if (passwordEncryptor.isEncrypted(password)) {
                password = passwordEncryptor.decrypt(password);
            }
            dataSource.setPassword(password);

            // 创建JdbcTemplate
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // 设置查询超时
            int timeout = options != null && options.getTimeout() != null ? options.getTimeout() : defaultTimeout;
            jdbcTemplate.setQueryTimeout(timeout / 1000);

            // 执行查询
            List<Map<String, Object>> results = jdbcTemplate.query(query, new RowMapper<Map<String, Object>>() {
                @Override
                public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                    ResultSetMetaData metaData = rs.getMetaData();
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }

                    return row;
                }
            });

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("数据源 {} 查询成功，返回 {} 行数据，耗时 {}ms",
                    config.getDatasourceName(), results.size(), executionTime);

            return MultiDatasourceQueryResponse.DatasourceQueryResult.builder()
                    .datasourceCode(config.getDatasourceCode())
                    .datasourceName(config.getDatasourceName())
                    .success(true)
                    .data(results)
                    .rowCount(results.size())
                    .rowsAffected(0)
                    .message("查询成功")
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("数据源 {} 查询失败: {}", config.getDatasourceName(), e.getMessage());

            return MultiDatasourceQueryResponse.DatasourceQueryResult.builder()
                    .datasourceCode(config.getDatasourceCode())
                    .datasourceName(config.getDatasourceName())
                    .success(false)
                    .data(new ArrayList<>())
                    .rowCount(0)
                    .rowsAffected(0)
                    .message("查询失败")
                    .error(e.getMessage())
                    .executionTime(executionTime)
                    .build();
        }
    }
}
