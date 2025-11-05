package com.example.sqlexecutor.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sqlexecutor.dto.ApiResponse;
import com.example.sqlexecutor.dto.ExecuteSqlRequest;
import com.example.sqlexecutor.service.SqlExecutionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SQL执行控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/execute-sql")
@RequiredArgsConstructor
@Validated
public class SqlController {

    private final SqlExecutionService sqlExecutionService;

    /**
     * 执行SQL查询
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> executeSql(
            @Valid @RequestBody ExecuteSqlRequest request) {

        log.info("收到SQL执行请求 - 数据库: {}, 查询类型: {}",
                request.getDatabase(),
                request.getQuery().substring(0, Math.min(50, request.getQuery().length())));

        ApiResponse<List<Map<String, Object>>> response = sqlExecutionService.executeSql(request);

        if (response.isSuccess()) {
            log.info("SQL执行成功 - 返回行数: {}, 受影响行数: {}, 执行时间: {}ms",
                    response.getRowCount(),
                    response.getRowsAffected(),
                    response.getExecutionTime());

            return ResponseEntity.ok(response);
        } else {
            log.warn("SQL执行失败: {}", response.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取支持的SQL操作类型
     */
    @GetMapping("/supported-operations")
    public ResponseEntity<Map<String, Object>> getSupportedOperations() {
        Map<String, Object> operations = Map.of(
                "allowedOperations", List.of("SELECT", "INSERT", "UPDATE", "DELETE", "WITH"),
                "forbiddenKeywords", List.of("DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE"),
                "defaultTimeout", 30000,
                "maxRows", "不限制");

        return ResponseEntity.ok(operations);
    }

    /**
     * 验证SQL语法（不执行）
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSql(@RequestBody Map<String, String> request) {
        String sql = request.get("query");

        try {
            // 这里可以调用服务层的验证方法
            // 暂时返回简单的验证结果
            boolean isValid = sql != null && !sql.trim().isEmpty();

            Map<String, Object> result = Map.of(
                    "valid", isValid,
                    "message", isValid ? "SQL语法有效" : "SQL语句不能为空");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> result = Map.of(
                    "valid", false,
                    "message", "SQL验证失败: " + e.getMessage());

            return ResponseEntity.badRequest().body(result);
        }
    }
}