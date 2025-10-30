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
import com.example.sqlexecutor.dto.MultiDatasourceExportRequest;
import com.example.sqlexecutor.dto.MultiDatasourceQueryRequest;
import com.example.sqlexecutor.dto.MultiDatasourceQueryResponse;
import com.example.sqlexecutor.service.ExcelExportService;
import com.example.sqlexecutor.service.MultiDatasourceQueryService;
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
    private final MultiDatasourceQueryService multiDatasourceQueryService;

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
     * 导出多数据源查询结果为ZIP文件
     */
    @PostMapping("/export-multi-datasource-excel")
    public ResponseEntity<?> exportMultiDatasourceExcel(@Valid @RequestBody MultiDatasourceExportRequest request) {
        log.info("收到多数据源Excel导出请求 - 数据源数量: {}, 文件名前缀: {}", 
                request.getDatasourceCodes().size(), request.getFilenamePrefix());

        long startTime = System.currentTimeMillis();

        try {
            // 构建多数据源查询请求
            MultiDatasourceQueryRequest queryRequest = new MultiDatasourceQueryRequest();
            queryRequest.setQuery(request.getQuery());
            queryRequest.setDatasourceCodes(request.getDatasourceCodes());
            queryRequest.setOptions(convertToExecuteSqlOptions(request.getOptions()));

            // 执行多数据源查询
            MultiDatasourceQueryResponse queryResponse = multiDatasourceQueryService
                    .executeMultiDatasourceQuery(queryRequest);

            if (queryResponse.getResults() == null || queryResponse.getResults().isEmpty()) {
                long executionTime = System.currentTimeMillis() - startTime;
                ApiResponse<Object> errorResponse = ApiResponse.<Object>builder()
                        .success(false)
                        .data(null)
                        .rowCount(0)
                        .rowsAffected(0)
                        .message("没有可导出的查询结果")
                        .executionTime(executionTime)
                        .metadata(null)
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 生成ZIP文件
            byte[] zipData = excelExportService.exportMultiDatasourcesToZip(queryResponse.getResults());

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            
            // 生成文件名
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            String filenamePrefix = request.getFilenamePrefix() != null && !request.getFilenamePrefix().isEmpty() 
                    ? request.getFilenamePrefix() 
                    : "multi_datasource_query";
            String filename = String.format("%s_%s.zip", filenamePrefix, timestamp);
            
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(zipData.length);

            log.info("多数据源Excel导出成功 - 文件大小: {} bytes, 包含 {} 个数据源, 执行时间: {}ms",
                    zipData.length, queryResponse.getResults().size(), System.currentTimeMillis() - startTime);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);

        } catch (Exception e) {
            log.error("多数据源Excel导出失败", e);
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

    /**
     * 转换多数据源导出请求选项为SQL执行选项
     */
    private ExecuteSqlRequest.QueryOptions convertToExecuteSqlOptions(MultiDatasourceExportRequest.QueryOptions options) {
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