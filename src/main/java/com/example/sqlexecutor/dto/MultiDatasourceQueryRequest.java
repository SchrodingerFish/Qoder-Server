package com.example.sqlexecutor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多数据源查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiDatasourceQueryRequest {

    /**
     * SQL查询语句
     */
    @NotBlank(message = "SQL查询语句不能为空")
    private String query;

    /**
     * 数据源编码列表
     */
    @NotEmpty(message = "数据源列表不能为空")
    @JsonProperty("datasourceCodes")
    private List<String> datasourceCodes;

    /**
     * 查询选项
     */
    private ExecuteSqlRequest.QueryOptions options;
}
