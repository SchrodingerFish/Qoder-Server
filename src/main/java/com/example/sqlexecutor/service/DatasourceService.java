package com.example.sqlexecutor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.sqlexecutor.dto.DatasourceTreeNode;
import com.example.sqlexecutor.entity.DatasourceCategory;
import com.example.sqlexecutor.entity.DatasourceConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据源管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatasourceService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取数据源树形结构
     */
    public List<DatasourceTreeNode> getDatasourceTree() {
        log.info("开始获取数据源树形结构");

        // 查询所有分类
        String categorySql = "SELECT id, category_name, category_code, parent_id, sort_order, " +
                "description, is_enabled, created_at, updated_at " +
                "FROM datasource_category WHERE is_enabled = true ORDER BY sort_order, id";

        List<DatasourceCategory> categories = jdbcTemplate.query(categorySql,
                new BeanPropertyRowMapper<>(DatasourceCategory.class));

        // 查询所有数据源
        String datasourceSql = "SELECT id, datasource_name, datasource_code, category_id, db_type, " +
                "host, port, database_name, username, password, jdbc_url, driver_class, " +
                "is_enabled, max_pool_size, min_idle, connection_timeout, description, " +
                "created_at, updated_at " +
                "FROM datasource_config WHERE is_enabled = true ORDER BY id";

        List<DatasourceConfig> datasources = jdbcTemplate.query(datasourceSql,
                new BeanPropertyRowMapper<>(DatasourceConfig.class));

        // 构建树形结构
        List<DatasourceTreeNode> tree = buildTree(categories, datasources);

        log.info("数据源树形结构获取成功，共 {} 个根节点", tree.size());
        return tree;
    }

    /**
     * 根据编码获取数据源配置
     */
    public DatasourceConfig getDatasourceByCode(String datasourceCode) {
        String sql = "SELECT id, datasource_name, datasource_code, category_id, db_type, " +
                "host, port, database_name, username, password, jdbc_url, driver_class, " +
                "is_enabled, max_pool_size, min_idle, connection_timeout, description, " +
                "created_at, updated_at " +
                "FROM datasource_config WHERE datasource_code = ? AND is_enabled = true";

        List<DatasourceConfig> results = jdbcTemplate.query(sql,
                new BeanPropertyRowMapper<>(DatasourceConfig.class), datasourceCode);

        if (results.isEmpty()) {
            throw new RuntimeException("数据源不存在或已禁用: " + datasourceCode);
        }

        return results.get(0);
    }

    /**
     * 批量获取数据源配置
     */
    public Map<String, DatasourceConfig> getDatasourcesByCodes(List<String> datasourceCodes) {
        if (datasourceCodes == null || datasourceCodes.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = datasourceCodes.stream()
                .map(code -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT id, datasource_name, datasource_code, category_id, db_type, " +
                "host, port, database_name, username, password, jdbc_url, driver_class, " +
                "is_enabled, max_pool_size, min_idle, connection_timeout, description, " +
                "created_at, updated_at " +
                "FROM datasource_config WHERE datasource_code IN (" + placeholders + ") AND is_enabled = true";

        List<DatasourceConfig> configs = jdbcTemplate.query(sql,
                new BeanPropertyRowMapper<>(DatasourceConfig.class),
                datasourceCodes.toArray());

        return configs.stream()
                .collect(Collectors.toMap(DatasourceConfig::getDatasourceCode, config -> config));
    }

    /**
     * 构建树形结构
     */
    private List<DatasourceTreeNode> buildTree(List<DatasourceCategory> categories,
            List<DatasourceConfig> datasources) {

        // 构建分类映射
        Map<Integer, DatasourceTreeNode> categoryMap = new HashMap<>();
        for (DatasourceCategory category : categories) {
            DatasourceTreeNode node = DatasourceTreeNode.builder()
                    .id("category_" + category.getId())
                    .label(category.getCategoryName())
                    .type("category")
                    .parentId(category.getParentId() != null ? "category_" + category.getParentId() : null)
                    .sortOrder(category.getSortOrder())
                    .description(category.getDescription())
                    .isEnabled(category.getIsEnabled())
                    .children(new ArrayList<>())
                    .build();

            categoryMap.put(category.getId(), node);
        }

        // 添加数据源到对应分类
        for (DatasourceConfig datasource : datasources) {
            DatasourceTreeNode datasourceNode = DatasourceTreeNode.builder()
                    .id("datasource_" + datasource.getId())
                    .label(datasource.getDatasourceName())
                    .type("datasource")
                    .datasourceCode(datasource.getDatasourceCode())
                    .parentId("category_" + datasource.getCategoryId())
                    .description(datasource.getDescription())
                    .isEnabled(datasource.getIsEnabled())
                    .build();

            DatasourceTreeNode parentCategory = categoryMap.get(datasource.getCategoryId());
            if (parentCategory != null) {
                parentCategory.addChild(datasourceNode);
            }
        }

        // 构建树形结构（找出根节点）
        List<DatasourceTreeNode> rootNodes = new ArrayList<>();
        for (DatasourceTreeNode node : categoryMap.values()) {
            if (node.getParentId() == null) {
                rootNodes.add(node);
            } else {
                // 将子分类添加到父分类
                String parentIdStr = node.getParentId().replace("category_", "");
                Integer parentId = Integer.parseInt(parentIdStr);
                DatasourceTreeNode parent = categoryMap.get(parentId);
                if (parent != null) {
                    parent.addChild(node);
                }
            }
        }

        // 按排序顺序排序
        sortTree(rootNodes);

        return rootNodes;
    }

    /**
     * 递归排序树节点
     */
    private void sortTree(List<DatasourceTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        nodes.sort((a, b) -> {
            // 分类排在前面
            if (!a.getType().equals(b.getType())) {
                return "category".equals(a.getType()) ? -1 : 1;
            }
            // 按sortOrder排序，如果没有则按label排序
            Integer orderA = a.getSortOrder() != null ? a.getSortOrder() : Integer.MAX_VALUE;
            Integer orderB = b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE;
            int orderCompare = orderA.compareTo(orderB);
            return orderCompare != 0 ? orderCompare : a.getLabel().compareTo(b.getLabel());
        });

        // 递归排序子节点
        for (DatasourceTreeNode node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }
}
