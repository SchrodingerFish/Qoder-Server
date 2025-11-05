# 🛠️ 多功能SQL查询工具（服务端）

一个基于 Spring Boot + PostgreSQL + Spring JDBC 的安全 SQL 查询执行服务，提供结构化的 SQL 查询执行和 Excel 导出功能。

## 📋 项目概述

### 核心功能

-   🔍 **安全 SQL 执行**：支持 SELECT、INSERT、UPDATE、DELETE、WITH 查询
-   📊 **Excel 导出**：将查询结果导出为 Excel 文件，支持 PostgreSQL 特有数据类型
-   🌲 **多数据源管理**：支持多个数据库连接配置和并行查询
-   📑 **多数据源并行查询**：同时查询多个数据源，结果独立展示
-   📦 **ZIP 压缩导出**：将多个数据源的 Excel 文件打包为 ZIP
-   ⚡ **超时控制**：可配置查询超时时间，防止长时间执行
-   📈 **行数限制**：可配置最大返回行数，避免内存溢出
-   🏥 **健康检查**：提供应用和数据库连接状态监控
-   🛡️ **安全防护**：SQL 注入检测和危险操作过滤
-   🎯 **全局异常处理**：统一的错误处理和用户友好的错误信息

### 技术特性

-   支持 PostgreSQL 特有数据类型（JSON、JSONB、UUID、几何类型等）
-   智能 Excel 格式化（日期、数字、小数格式）
-   多数据源动态配置和连接池管理
-   线程池并发查询，提高多数据源查询效率
-   ZIP 压缩打包多个 Excel 文件
-   连接池监控和管理
-   详细的执行元数据返回
-   RESTful API 设计

## 🏗️ 技术架构

### 技术栈

-   **后端框架**：Spring Boot 3.2.0
-   **数据库**：PostgreSQL 12+
-   **数据访问**：Spring JDBC + HikariCP 连接池
-   **文档导出**：Apache POI
-   **代码简化**：Lombok
-   **构建工具**：Maven
-   **Java 版本**：Java 17+

### 架构设计

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controller    │───▶│     Service     │───▶│   Database      │
│                 │    │                 │    │                 │
│ - SqlController │    │ - SqlExecution  │    │ - PostgreSQL    │
│ - ExcelExport   │    │ - ExcelExport   │    │ - HikariCP      │
│ - Health        │    │ - HealthCheck   │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│      DTO        │    │   Exception     │    │     Config      │
│                 │    │                 │    │                 │
│ - ApiResponse   │    │ - GlobalHandler │    │ - DatabaseConfig│
│ - Request       │    │ - SqlException  │    │ - WebConfig     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🚀 快速开始

### 配合前端使用
-   [多功能 SQL 查询工具（客户端）](https://github.com/SchrodingerFish/Qoder)

### 环境要求

-   Java 17+
-   Maven 3.6+
-   PostgreSQL 12+

### 安装和运行

1. **克隆项目**

```bash
git clone <repository-url>
cd Qoder-Server
```

2. **配置数据库**

修改 `src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
    datasource:
        url: jdbc:postgresql://localhost:5432/your_database
        username: your_username
        password: your_password
```

3. **编译项目**

```bash
mvn clean compile
```

4. **运行应用**

```bash
mvn spring-boot:run
```

5. **验证运行**

```bash
curl http://localhost:8080/api/health
```

## 📚 API 文档

### SQL 执行接口

#### 执行 SQL 查询

```http
POST /api/execute-sql
Content-Type: application/json

{
  "query": "SELECT * FROM users LIMIT 10",
  "database": "main",
  "options": {
    "timeout": 30000,
    "format": "json",
    "includeMetadata": true,
    "maxRows": 1000
  }
}
```

**响应示例：**

```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "username": "user1",
            "email": "user1@example.com"
        }
    ],
    "rowCount": 1,
    "rowsAffected": 0,
    "message": "查询成功",
    "executionTime": 45,
    "metadata": {
        "queryType": "SELECT",
        "database": "main",
        "columns": []
    }
}
```

#### 获取支持的操作

```http
GET /api/execute-sql/supported-operations
```

#### SQL 语法验证

```http
POST /api/execute-sql/validate
Content-Type: application/json

{
  "query": "SELECT COUNT(*) FROM users"
}
```

### Excel 导出接口

#### 导出查询结果为 Excel

```http
POST /api/export-excel
Content-Type: application/json

{
  "timestamp": "2024-01-01T00:00:00Z",
  "query": "SELECT * FROM users",
  "database": "main",
  "filename": "用户数据",
  "options": {
    "timeout": 30000,
    "maxRows": 10000
  }
}
```

**响应：** Excel 文件下载

### 健康检查接口

#### 基本健康检查

```http
GET /api/health
```

#### 详细健康检查

```http
GET /api/health/detailed
```

#### 数据库连接检查

```http
GET /api/health/database
```

## ⚙️ 配置说明

### 应用配置

```yaml
# 服务器配置
server:
    port: 8080

# SQL执行器配置
sql-executor:
    # 默认查询超时时间（毫秒）
    default-timeout: 30000
    # 最大返回行数（-1表示不限制）
    max-rows: 10000
    # 允许的SQL操作类型
    allowed-operations: SELECT,INSERT,UPDATE,DELETE,WITH
    # 禁止的关键字（可为空）
    forbidden-keywords: ''

# 数据库连接池配置
spring:
    datasource:
        hikari:
            maximum-pool-size: 20
            minimum-idle: 5
            idle-timeout: 300000
            connection-timeout: 30000
            max-lifetime: 1800000
```

### 安全配置

-   **允许的操作**：SELECT、INSERT、UPDATE、DELETE、WITH
-   **SQL 注入防护**：检测危险模式和多语句执行
-   **超时控制**：防止长时间执行的查询
-   **行数限制**：防止内存溢出

## 🔧 项目结构

```
src/main/java/com/example/sqlexecutor/
├── config/                     # 配置类
│   ├── DatabaseConfig.java     # 数据库配置
│   ├── WebConfig.java          # Web配置（CORS等）
│   └── HttpErrorMessages.java  # HTTP错误消息映射
├── controller/                 # 控制器层
│   ├── SqlController.java      # SQL执行接口
│   ├── ExcelExportController.java # Excel导出接口
│   ├── DatasourceController.java # 数据源管理接口
│   └── HealthController.java   # 健康检查接口
├── dto/                        # 数据传输对象
│   ├── ApiResponse.java        # 通用API响应
│   ├── ExecuteSqlRequest.java  # SQL执行请求
│   ├── ExportExcelRequest.java # Excel导出请求
│   ├── MultiDatasourceExportRequest.java # 多数据源导出请求
│   ├── MultiDatasourceQueryRequest.java # 多数据源查询请求
│   ├── MultiDatasourceQueryResponse.java # 多数据源查询响应
│   ├── DatasourceTreeNode.java # 数据源树节点
│   └── HealthResponse.java     # 健康检查响应
├── exception/                  # 异常处理
│   ├── GlobalExceptionHandler.java # 全局异常处理器
│   ├── SqlExecutionException.java  # SQL执行异常
│   └── InvalidSqlException.java    # 无效SQL异常
├── service/                    # 服务层
│   ├── SqlExecutionService.java    # SQL执行服务
│   ├── ExcelExportService.java     # Excel导出服务
│   └── HealthCheckService.java     # 健康检查服务
└── SqlExecutorApplication.java # 主启动类
```

## 📊 支持的数据类型

### PostgreSQL 数据类型支持

-   **基本类型**：String、Integer、Long、BigDecimal、Boolean
-   **日期时间**：Date、Time、Timestamp、LocalDateTime、OffsetDateTime
-   **PostgreSQL 特有**：JSON、JSONB、UUID、Point、Circle、Polygon、Interval
-   **数组类型**：各种数组类型的智能显示
-   **字节数据**：BYTEA 类型的十六进制显示

### Excel 格式化

-   **日期格式**：yyyy-mm-dd hh:mm:ss
-   **数字格式**：#,##0（千分位分隔）
-   **小数格式**：#,##0.00
-   **JSON 格式化**：长 JSON 自动截断
-   **数组处理**：小数组显示内容，大数组显示长度

## 🔍 监控和健康检查

### Actuator 端点

-   `/actuator/health` - 应用健康状态
-   `/actuator/info` - 应用信息
-   `/actuator/metrics` - 应用指标

### 自定义健康检查

-   **数据库连接**：连接状态和响应时间
-   **连接池状态**：活跃/空闲连接数
-   **应用信息**：版本、启动时间、运行时间

## 🛠️ 开发指南

### 本地开发

1. 安装必要工具（Java 17+、Maven、PostgreSQL）
2. 配置 IDE 的 Lombok 插件
3. 配置数据库连接
4. 运行测试：`mvn test`
5. 启动应用：`mvn spring-boot:run`

### 测试数据

项目包含测试 SQL 脚本 `src/main/resources/test-data.sql`，包含用户表的创建和索引。

### API 测试

推荐使用 Postman 或 curl 进行 API 测试：

```bash
# 测试健康检查
curl http://localhost:8080/api/health

# 测试SQL执行
curl -X POST http://localhost:8080/api/execute-sql \
  -H "Content-Type: application/json" \
  -d '{"query":"SELECT 1","database":"main"}'
```

## 🔒 安全注意事项

1. **数据库权限**：建议使用最小权限的数据库用户
2. **网络安全**：生产环境建议配置防火墙和 VPN
3. **SQL 注入**：已内置基础防护，但请谨慎处理用户输入
4. **认证授权**：当前版本未包含认证，生产使用需要额外集成
5. **审计日志**：建议在生产环境启用 SQL 执行日志

## 🚀 部署指南

### 打包应用

```bash
mvn clean package
```

### 运行 JAR 包

```bash
java -jar target/sql-executor-1.0.0.jar
```

### Docker 部署（含前端）

参考 [Docker 部署指南](Docker/README.md) 进行容器化部署。

## 📝 更新日志

### v1.0.0 (当前版本)

-   ✅ 基础 SQL 执行功能
-   ✅ Excel 单数据源导出功能
-   ✅ Excel 多数据源zip压缩后导出
-   ✅ PostgreSQL 数据类型支持
-   ✅ 多数据源并行查询支持
-   ✅ 健康检查接口
-   ✅ 全局异常处理
-   ✅ 安全防护机制

### 计划功能

-   🔄 认证授权集成
-   🔄 审计日志功能
-   🔄 异步查询执行
-   🔄 查询结果缓存

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

-   创建 Issue
-   发送 Pull Request
-   邮件联系项目维护者 <EMAIL>schrodingersfish@outlook.com</EMAIL>
 
---

**注意**：本项目仅供学习和开发使用，生产环境使用前请进行充分的安全评估和测试。
## 📄 许可证

本项目采用 [MIT 许可证](LICENSE)。

---

<div align="center">

**如果这个项目对您有帮助，请给我一个 ⭐️**

Made with ❤️ by SchrodingerFish

</div>
