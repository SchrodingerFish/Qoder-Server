package com.example.sqlexecutor.service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.example.sqlexecutor.dto.ApiResponse;
import com.example.sqlexecutor.dto.ExecuteSqlRequest;
import com.example.sqlexecutor.exception.InvalidSqlException;
import com.example.sqlexecutor.exception.SqlExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutionService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${sql-executor.default-timeout:30000}")
    private int defaultTimeout;

    @Value("${sql-executor.max-rows:10000}")
    private int maxRows;

    @Value("#{'${sql-executor.allowed-operations}'.split(',')}")
    private List<String> allowedOperations;

    @Value("#{'${sql-executor.forbidden-keywords}'.split(',')}")
    private List<String> forbiddenKeywords;

    // SQL操作类型判断模式
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile("^\\s*INSERT", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*UPDATE", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("^\\s*DELETE", Pattern.CASE_INSENSITIVE);
    private static final Pattern WITH_PATTERN = Pattern.compile("^\\s*WITH", Pattern.CASE_INSENSITIVE);

    /**
     * 执行SQL查询
     */
    public ApiResponse<List<Map<String, Object>>> executeSql(ExecuteSqlRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证SQL
            validateSql(request.getQuery());

            // 获取执行选项
            ExecuteSqlRequest.QueryOptions options = request.getOptions();
            if (options == null) {
                options = new ExecuteSqlRequest.QueryOptions();
            }

            // 设置查询超时
            int timeout = options.getTimeout() != null ? options.getTimeout() : defaultTimeout;
            jdbcTemplate.setQueryTimeout(timeout / 1000); // 转换为秒

            // 判断SQL类型并执行
            String queryType = determineQueryType(request.getQuery());

            if ("SELECT".equals(queryType) || "WITH".equals(queryType)) {
                return executeSelectQuery(request, options, startTime, queryType);
            } else {
                return executeUpdateQuery(request, options, startTime, queryType);
            }

        } catch (InvalidSqlException e) {
            log.warn("SQL验证失败: {}", e.getMessage());
            long executionTime = System.currentTimeMillis() - startTime;
            return ApiResponse.error("SQL验证失败: " + e.getMessage());

        } catch (DataAccessException e) {
            // 直接重新抛出DataAccessException，让全局异常处理器处理
            throw e;

        } catch (SqlExecutionException e) {
            // 直接重新抛出SqlExecutionException，让全局异常处理器处理
            throw e;

        }
    }

    /**
     * 执行查询语句
     */
    private ApiResponse<List<Map<String, Object>>> executeSelectQuery(
            ExecuteSqlRequest request,
            ExecuteSqlRequest.QueryOptions options,
            long startTime,
            String queryType) {

        try {
            // 获取原始SQL语句
            String sql = request.getQuery();

            // 只有在配置了maxRows且大于0时才进行行数限制
            if (maxRows > 0) {
                int maxRowsLimit = options.getMaxRows() != null ? Math.min(options.getMaxRows(), maxRows) : maxRows;

                // PostgreSQL LIMIT语法（仅在没有LIMIT时添加）
                if (!sql.toLowerCase().contains("limit")) {
                    sql = sql + " LIMIT " + maxRowsLimit;
                }
            }

            List<Map<String, Object>> results = jdbcTemplate.query(sql, new RowMapper<Map<String, Object>>() {
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

            // 构建响应
            if (options.getIncludeMetadata() != null && options.getIncludeMetadata()) {
                ApiResponse.Metadata metadata = buildMetadata(request.getQuery(), request.getDatabase(), queryType);
                return ApiResponse.success(results, results.size(), 0, "查询成功", executionTime, metadata);
            } else {
                return ApiResponse.success(results, results.size(), 0, "查询成功", executionTime);
            }

        } catch (DataAccessException e) {
            // 直接重新抛出DataAccessException，让全局异常处理器处理
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            throw new SqlExecutionException("查询执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行更新语句（INSERT, UPDATE, DELETE）
     */
    private ApiResponse<List<Map<String, Object>>> executeUpdateQuery(
            ExecuteSqlRequest request,
            ExecuteSqlRequest.QueryOptions options,
            long startTime,
            String queryType) {

        try {
            int rowsAffected = jdbcTemplate.update(request.getQuery());
            long executionTime = System.currentTimeMillis() - startTime;

            // 构建响应
            List<Map<String, Object>> emptyData = new ArrayList<>();
            String message = String.format("%s操作成功", queryType);

            if (options.getIncludeMetadata() != null && options.getIncludeMetadata()) {
                ApiResponse.Metadata metadata = buildMetadata(request.getQuery(), request.getDatabase(), queryType);
                return ApiResponse.success(emptyData, 0, rowsAffected, message, executionTime, metadata);
            } else {
                return ApiResponse.success(emptyData, 0, rowsAffected, message, executionTime);
            }

        } catch (DataAccessException e) {
            // 直接重新抛出DataAccessException，让全局异常处理器处理
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            throw new SqlExecutionException("更新操作执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证SQL语句
     */
    public void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new InvalidSqlException("SQL语句不能为空");
        }

        String upperSql = sql.toUpperCase().trim();

        // 检查禁止的关键字
        // for (String forbidden : forbiddenKeywords) {
        // if (upperSql.contains(forbidden.toUpperCase())) {
        // throw new InvalidSqlException("SQL包含禁止的关键字: " + forbidden);
        // }
        // }

        // 检查允许的操作类型
        boolean isAllowed = false;
        for (String allowed : allowedOperations) {
            if (upperSql.startsWith(allowed.toUpperCase())) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new InvalidSqlException("不支持的SQL操作类型，仅允许: " + String.join(", ", allowedOperations));
        }

        // 基本SQL注入检查
        if (containsSqlInjectionPatterns(sql)) {
            throw new InvalidSqlException("SQL语句包含潜在的安全风险");
        }
    }

    /**
     * 判断SQL语句类型
     */
    private String determineQueryType(String sql) {
        if (SELECT_PATTERN.matcher(sql).find()) {
            return "SELECT";
        } else if (INSERT_PATTERN.matcher(sql).find()) {
            return "INSERT";
        } else if (UPDATE_PATTERN.matcher(sql).find()) {
            return "UPDATE";
        } else if (DELETE_PATTERN.matcher(sql).find()) {
            return "DELETE";
        } else if (WITH_PATTERN.matcher(sql).find()) {
            return "WITH";
        } else {
            return "UNKNOWN";
        }
    }

    /**
     * 构建元数据信息
     */
    private ApiResponse.Metadata buildMetadata(String sql, String database, String queryType) {
        return ApiResponse.Metadata.builder()
                .queryType(queryType)
                .database(database)
                .columns(new ArrayList<>()) // 实际项目中可以通过ResultSetMetaData获取
                .executionPlan(new HashMap<>()) // 可以通过EXPLAIN获取执行计划
                .build();
    }

    /**
     * 简单的SQL注入检查
     */
    private boolean containsSqlInjectionPatterns(String sql) {
        String lowerSql = sql.toLowerCase();

        // 检查一些常见的SQL注入模式
        String[] injectionPatterns = {
                "/\\*",
                "\\*/",
                "--",
                // "xp_",
                // "sp_",
                // "union[\\s]+select", // 更精确的UNION SELECT检查
                // "insert[\\s]+into", // 更精确的INSERT INTO检查
                // "delete[\\s]+from", // 更精确的DELETE FROM检查
                // "create[\\s]+table",
                // "drop[\\s]+table",
                // "alter[\\s]+table",
                // "exec\\s*\\(",
                // "execute\\s*\\("
        };

        for (String pattern : injectionPatterns) {
            if (lowerSql.matches(".*" + pattern + ".*")) {
                return true;
            }
        }

        // 检查可能的SQL注入关键模式
        // 1. 检查是否有多个语句（以分号分隔，但排除末尾分号）
        String trimmedSql = sql.trim();
        if (trimmedSql.endsWith(";")) {
            trimmedSql = trimmedSql.substring(0, trimmedSql.length() - 1);
        }
        if (trimmedSql.contains(";")) {
            return true; // 包含多个SQL语句
        }

        // 2. 检查危险的单引号模式（但排除正常的字符串值）
        // 检查单引号后直接跟SQL关键字的情况（如 'OR 1=1）
        if (lowerSql.matches(".*'\\s*(or|and|union|select|insert|update|delete|drop|create|alter).*")) {
            return true;
        }

        // 3. 检查其他危险模式
        if (lowerSql.matches(".*(1\\s*=\\s*1|0\\s*=\\s*0).*")) {
            return true; // 常见的注入测试条件
        }

        return false;
    }
}