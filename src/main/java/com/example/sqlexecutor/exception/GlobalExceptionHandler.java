package com.example.sqlexecutor.exception;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.example.sqlexecutor.config.HttpErrorMessages;
import com.example.sqlexecutor.dto.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * 处理SQL执行异常
         */
        @ExceptionHandler(SqlExecutionException.class)
        public ResponseEntity<ApiResponse<Object>> handleSqlExecutionException(
                        SqlExecutionException ex, WebRequest request) {

                log.error("SQL执行异常: ", ex);

                // 提取更详细的错误信息
                String detailedMessage = extractDetailedErrorMessage(ex);

                ApiResponse<Object> response = ApiResponse.error(detailedMessage);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        /**
         * 处理无效SQL异常
         */
        @ExceptionHandler(InvalidSqlException.class)
        public ResponseEntity<ApiResponse<Object>> handleInvalidSqlException(
                        InvalidSqlException ex, WebRequest request) {

                log.warn("无效SQL异常: {}", ex.getMessage());

                ApiResponse<Object> response = ApiResponse.error(
                                HttpErrorMessages.getErrorMessage(400, ex.getMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * 处理数据访问异常
         */
        @ExceptionHandler(DataAccessException.class)
        public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
                        DataAccessException ex, WebRequest request) {

                log.error("数据访问异常: ", ex);

                // 提取详细的PostgreSQL错误信息
                String detailedMessage = extractSqlErrorMessage(ex);

                ApiResponse<Object> response = ApiResponse.error(detailedMessage);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * 处理SQL超时异常
         */
        @ExceptionHandler(SQLTimeoutException.class)
        public ResponseEntity<ApiResponse<Object>> handleSQLTimeoutException(
                        SQLTimeoutException ex, WebRequest request) {

                log.warn("SQL执行超时: {}", ex.getMessage());

                ApiResponse<Object> response = ApiResponse.error(
                                HttpErrorMessages.getErrorMessage(408, "SQL执行超时，请稍后重试"));

                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(response);
        }

        /**
         * 处理参数验证异常
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<Object>> handleValidationException(
                        MethodArgumentNotValidException ex, WebRequest request) {

                log.warn("参数验证失败: {}", ex.getMessage());

                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach((error) -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                String message = "参数验证失败: " + errors.toString();
                ApiResponse<Object> response = ApiResponse.error(
                                HttpErrorMessages.getErrorMessage(400, message));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * 处理绑定异常
         */
        @ExceptionHandler(BindException.class)
        public ResponseEntity<ApiResponse<Object>> handleBindException(
                        BindException ex, WebRequest request) {

                log.warn("参数绑定失败: {}", ex.getMessage());

                List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.toList());

                String message = "参数验证失败: " + String.join(", ", errors);
                ApiResponse<Object> response = ApiResponse.error(
                                HttpErrorMessages.getErrorMessage(400, message));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * 处理非法参数异常
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
                        IllegalArgumentException ex, WebRequest request) {

                log.warn("非法参数异常: {}", ex.getMessage());

                ApiResponse<Object> response = ApiResponse.error(
                                HttpErrorMessages.getErrorMessage(400, "请求参数错误: " + ex.getMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        /**
         * 处理通用异常
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Object>> handleGenericException(
                        Exception ex, WebRequest request) {

                log.error("未处理的异常: ", ex);

                ApiResponse<Object> response = ApiResponse.error(
                                HttpErrorMessages.getErrorMessage(500, "服务器内部错误，请稍后重试"));

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        /**
         * 提取SQL执行异常的详细错误信息
         */
        private String extractDetailedErrorMessage(SqlExecutionException ex) {
                Throwable cause = ex.getCause();

                // 检查是否有DataAccessException
                if (cause instanceof DataAccessException) {
                        return extractSqlErrorMessage((DataAccessException) cause);
                }

                return ex.getMessage();
        }

        /**
         * 提取PostgreSQL数据库错误信息
         */
        private String extractSqlErrorMessage(DataAccessException ex) {
                Throwable cause = ex.getCause();

                // 检查是否是PostgreSQL异常（通过类名判断）
                if (cause != null && cause.getClass().getName().equals("org.postgresql.util.PSQLException")) {
                        String message = "SQL语法错误: " + cause.getMessage();

                        // 尝试获取SQL状态码（通过反射）
                        try {
                                java.lang.reflect.Method getSqlStateMethod = cause.getClass().getMethod("getSQLState");
                                String sqlState = (String) getSqlStateMethod.invoke(cause);
                                if (sqlState != null && !sqlState.isEmpty()) {
                                        message += " (错误代码: " + sqlState + ")";
                                }
                        } catch (Exception ignored) {
                                // 忽略反射异常
                        }

                        return message;
                }

                // 其他SQL异常
                if (cause instanceof SQLException) {
                        SQLException sqlEx = (SQLException) cause;
                        return "SQL执行错误: " + sqlEx.getMessage();
                }

                // 默认错误信息
                return "数据库操作失败: " + ex.getMessage();
        }
}