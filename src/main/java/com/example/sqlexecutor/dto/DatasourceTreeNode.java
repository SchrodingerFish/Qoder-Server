package com.example.sqlexecutor.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源树形节点DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasourceTreeNode {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点名称
     */
    private String label;

    /**
     * 节点类型: category(分类) 或 datasource(数据源)
     */
    private String type;

    /**
     * 数据源编码（仅数据源节点有效）
     */
    @JsonProperty("datasourceCode")
    private String datasourceCode;

    /**
     * 父节点ID
     */
    @JsonProperty("parentId")
    private String parentId;

    /**
     * 排序顺序
     */
    @JsonProperty("sortOrder")
    private Integer sortOrder;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 是否启用
     */
    @JsonProperty("isEnabled")
    private Boolean isEnabled;

    /**
     * 子节点列表
     */
    private List<DatasourceTreeNode> children;

    /**
     * 添加子节点
     */
    public void addChild(DatasourceTreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
