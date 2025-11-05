package com.example.sqlexecutor.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源分类实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasourceCategory {

    /**
     * 分类ID
     */
    private Integer id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 父级分类ID
     */
    private Integer parentId;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
