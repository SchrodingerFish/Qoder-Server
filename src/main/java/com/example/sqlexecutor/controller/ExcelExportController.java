package com.example.sqlexecutor.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sqlexecutor.dto.ApiResponse;
import com.example.sqlexecutor.dto.ExecuteSqlRequest;
import com.example.sqlexecutor.dto.ExportExcelRequest;
import com.example.sqlexecutor.service.ExcelExportService;
import com.example.sqlexecutor.service.SqlExecutionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Excel导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class ExcelExportController {

    private final SqlExecutionService sqlExecutionService;
    private final ExcelExportService excelExportService;

    /**
     * 导出Excel文件
     */
    @PostMapping("/export-excel")
    public ResponseEntity<?> exportExcel(@Valid @RequestBody ExportExcelRequest request) {
        log.info("收到Excel导出请求 - 数据库: {}, 文件名: {}", request.getDatabase(), request.getFilename());

        long startTime = System.currentTimeMillis();

        try {
            // 构建SQL执行请求
            ExecuteSqlRequest sqlRequest = new ExecuteSqlRequest(
                    request.getQuery(),
                    request.getDatabase(),
                    convertToExecuteSqlOptions(request.getOptions()));

            // 执行SQL查询
            ApiResponse<List<Map<String, Object>>> sqlResponse = sqlExecutionService.executeSql(sqlRequest);

            if (!sqlResponse.isSuccess()) {
                // 如果SQL执行失败，返回错误响应
                long executionTime = System.currentTimeMillis() - startTime;
                ApiResponse<Object> errorResponse = ApiResponse.<Object>builder()
                        .success(false)
                        .data(null)
                        .rowCount(0)
                        .rowsAffected(0)
                        .message(sqlResponse.getMessage())
                        .executionTime(executionTime)
                        .metadata(null)
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 生成Excel文件
            byte[] excelData = excelExportService.exportToExcel(sqlResponse.getData(), "查询结果");

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", request.getFilename() + ".xlsx");
            headers.setContentLength(excelData.length);

            log.info("Excel导出成功 - 文件大小: {} bytes, 执行时间: {}ms",
                    excelData.length, System.currentTimeMillis() - startTime);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            long executionTime = System.currentTimeMillis() - startTime;
            ApiResponse<Object> errorResponse = ApiResponse.<Object>builder()
                    .success(false)
                    .data(null)
                    .rowCount(0)
                    .rowsAffected(0)
                    .message("导出失败: " + e.getMessage())
                    .executionTime(executionTime)
                    .metadata(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 转换Excel请求选项为SQL执行选项
     */
    private ExecuteSqlRequest.QueryOptions convertToExecuteSqlOptions(ExportExcelRequest.QueryOptions options) {
        if (options == null) {
            return new ExecuteSqlRequest.QueryOptions();
        }

        ExecuteSqlRequest.QueryOptions sqlOptions = new ExecuteSqlRequest.QueryOptions();
        sqlOptions.setTimeout(options.getTimeout());
        sqlOptions.setFormat(options.getFormat());
        sqlOptions.setIncludeMetadata(options.getIncludeMetadata());
        sqlOptions.setMaxRows(options.getMaxRows());

        return sqlOptions;
    }
}