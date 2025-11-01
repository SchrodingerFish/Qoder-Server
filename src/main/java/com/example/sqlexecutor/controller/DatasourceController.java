package com.example.sqlexecutor.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.sqlexecutor.dto.DatasourceTreeNode;
import com.example.sqlexecutor.dto.MultiDatasourceQueryRequest;
import com.example.sqlexecutor.dto.MultiDatasourceQueryResponse;
import com.example.sqlexecutor.service.DatasourceService;
import com.example.sqlexecutor.service.MultiDatasourceQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据源管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;
    private final MultiDatasourceQueryService multiDatasourceQueryService;

    /**
     * 获取数据源树形结构
     */
    @GetMapping("/tree")
    public ResponseEntity<List<DatasourceTreeNode>> getDatasourceTree() {
        log.info("收到获取数据源树形结构请求");

        try {
            List<DatasourceTreeNode> tree = datasourceService.getDatasourceTree();
            log.info("数据源树形结构获取成功，返回 {} 个根节点", tree.size());
            return ResponseEntity.ok(tree);

        } catch (Exception e) {
            log.error("获取数据源树形结构失败", e);
            throw e;
        }
    }

    /**
     * 执行多数据源并行查询
     */
    @PostMapping("/multi-query")
    public ResponseEntity<MultiDatasourceQueryResponse> executeMultiQuery(
            @Valid @RequestBody MultiDatasourceQueryRequest request) {

        log.info("收到多数据源查询请求 - 数据源数量: {}, SQL: {}",
                request.getDatasourceCodes().size(),
                request.getQuery().substring(0, Math.min(50, request.getQuery().length())));

        try {
            MultiDatasourceQueryResponse response = multiDatasourceQueryService
                    .executeMultiDatasourceQuery(request);

            // 总是返回OK状态，让前端根据每个数据源的结果来显示
            // 即使整体标记为失败，也要返回详细的结果信息
            if (response.isSuccess()) {
                log.info("多数据源查询执行成功: {}", response.getMessage());
            } else {
                log.warn("多数据源查询执行完成，但包含错误: {}", response.getMessage());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("多数据源查询执行异常", e);
            throw e;
        }
    }
}
