package com.example.sqlexecutor.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasourceConfig {

    /**
     * 数据源ID
     */
    private Integer id;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 数据源编码
     */
    private String datasourceCode;

    /**
     * 所属分类ID
     */
    private Integer categoryId;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 主机地址
     */
    private String host;

    /**
     * 端口号
     */
    private Integer port;

    /**
     * 数据库名称
     */
    private String databaseName;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * JDBC连接URL
     */
    private String jdbcUrl;

    /**
     * 驱动类名
     */
    private String driverClass;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 最大连接池大小
     */
    private Integer maxPoolSize;

    /**
     * 最小空闲连接数
     */
    private Integer minIdle;

    /**
     * 连接超时时间(毫秒)
     */
    private Integer connectionTimeout;

    /**
     * 数据源描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
