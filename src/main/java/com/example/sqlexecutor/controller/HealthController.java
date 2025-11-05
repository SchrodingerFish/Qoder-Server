package com.example.sqlexecutor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sqlexecutor.dto.HealthResponse;
import com.example.sqlexecutor.service.HealthCheckService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 健康检查控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckService healthCheckService;

    /**
     * 基本健康检查
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        log.debug("执行健康检查");

        try {
            HealthResponse response = healthCheckService.getBasicHealth();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("健康检查失败", e);
            HealthResponse response = HealthResponse.builder()
                    .status("error")
                    .timestamp(java.time.ZonedDateTime.now())
                    .build();
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 详细健康检查
     */
    @GetMapping("/detailed")
    public ResponseEntity<HealthResponse> detailedHealth() {
        log.debug("执行详细健康检查");

        try {
            HealthResponse response = healthCheckService.getDetailedHealth();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("详细健康检查失败", e);
            HealthResponse response = HealthResponse.builder()
                    .status("error")
                    .timestamp(java.time.ZonedDateTime.now())
                    .build();
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * 数据库连接检查
     */
    @GetMapping("/database")
    public ResponseEntity<HealthResponse.DatabaseStatus> databaseHealth() {
        log.debug("执行数据库连接检查");

        try {
            HealthResponse.DatabaseStatus status = healthCheckService.getDatabaseStatus();
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            HealthResponse.DatabaseStatus status = HealthResponse.DatabaseStatus.builder()
                    .status("error")
                    .responseTime(-1L)
                    .build();
            return ResponseEntity.status(503).body(status);
        }
    }
}