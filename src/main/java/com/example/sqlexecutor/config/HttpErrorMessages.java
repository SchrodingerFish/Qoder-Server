package com.example.sqlexecutor.config;

import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * HTTP错误消息映射
 */
@Component
public class HttpErrorMessages {

    /**
     * HTTP状态码错误映射
     */
    public static final Map<Integer, String> ERROR_MESSAGES = Map.of(
            400, "SQL语法错误或请求参数无效",
            401, "认证失败，请检查登录状态",
            403, "权限不足，无法执行此查询",
            404, "API接口不存在，请检查服务器配置",
            408, "请求超时，请稍后重试",
            429, "请求过于频繁，请稍后重试",
            500, "服务器内部错误，请稍后重试",
            502, "网关错误，请检查服务器状态",
            503, "服务暂时不可用，请稍后重试",
            504, "网关超时，请稍后重试");

    /**
     * 获取错误消息
     */
    public static String getErrorMessage(int statusCode) {
        return ERROR_MESSAGES.getOrDefault(statusCode, "未知错误");
    }

    /**
     * 获取错误消息（带默认值）
     */
    public static String getErrorMessage(int statusCode, String defaultMessage) {
        return ERROR_MESSAGES.getOrDefault(statusCode, defaultMessage);
    }
}