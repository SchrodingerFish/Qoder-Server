# 数据源密码加密使用指南

## 📌 概述

为了提高安全性，系统支持对数据库中存储的数据源密码进行 **AES-256-GCM 加密**。

## 🔐 加密算法

-   **算法**: AES-256-GCM
-   **密钥**: 可配置（通过 `application.yml`）
-   **特性**:
    -   自动识别明文和密文
    -   向后兼容（支持明文密码）
    -   高强度加密

## 🚀 快速开始

### 步骤 1: 配置加密密钥

编辑 `src/main/resources/application.yml`：

```yaml
sql-executor:
    encryption:
        # 建议生成一个强密钥（32位以上）
        secret-key: 'YourStrongSecretKeyHere123456789012'
```

**重要提示**:

-   生产环境务必修改默认密钥
-   密钥一旦设置，不要随意修改（会导致已加密密码无法解密）
-   建议将密钥配置在环境变量或外部配置中心

### 步骤 2: 生成加密密码

#### 方法 A: 使用加密工具类

```bash
cd Qoder-Server

# 编译项目
mvn clean package

# 加密密码
java -cp target/sql-executor-1.0.0.jar \
  com.example.sqlexecutor.util.PasswordEncryptionTool \
  encrypt "Qcj8813818@#"
```

或者使用 Maven 运行：

```bash
mvn spring-boot:run \
  -Dstart-class=com.example.sqlexecutor.util.PasswordEncryptionTool \
  -Dspring-boot.run.arguments="encrypt Qcj8813818@#"
```

#### 方法 B: 在代码中直接测试

运行 `PasswordEncryptionTool.java` 的 main 方法，会显示示例加密结果。

#### 输出示例：

```
========================================
密码加密/解密工具
========================================

原始密码: Qcj8813818@#
加密密码: kXm8vN2pQ1rT5wY6zAb3cD4eF7gH8iJ9kL0mN1oP2qR3sT4uV5wX6yZ7aB8cD9eF==

SQL更新语句示例:
UPDATE datasource_config SET password = 'kXm8vN2pQ1rT5wY6zAb3cD4eF7gH8iJ9kL0mN1oP2qR3sT4uV5wX6yZ7aB8cD9eF==' WHERE datasource_code = 'your_code';

========================================
```

### 步骤 3: 更新数据库密码

使用生成的加密密码更新数据库：

```sql
-- 更新单个数据源
UPDATE datasource_config
SET password = 'kXm8vN2pQ1rT5wY6zAb3cD4eF7gH8iJ9kL0mN1oP2qR3sT4uV5wX6yZ7aB8cD9eF=='
WHERE datasource_code = 'main_business';

-- 或批量更新（如果所有数据源使用相同密码）
UPDATE datasource_config
SET password = 'kXm8vN2pQ1rT5wY6zAb3cD4eF7gH8iJ9kL0mN1oP2qR3sT4uV5wX6yZ7aB8cD9eF==';
```

### 步骤 4: 验证

启动应用后，系统会自动：

1. 检测密码是否加密（通过 Base64 格式判断）
2. 如果是加密密码，自动解密后使用
3. 如果是明文密码，直接使用（向后兼容）

查看日志确认：

```
INFO  - 开始查询数据源: 主业务数据库 [main_business]
```

## 🔧 高级配置

### 生成强密钥

使用工具生成随机密钥：

```bash
mvn spring-boot:run \
  -Dstart-class=com.example.sqlexecutor.util.PasswordEncryptionTool
```

输出会包含随机密钥：

```
生成随机密钥:
  a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6==
```

### 环境变量配置

在生产环境中，建议通过环境变量配置密钥：

```yaml
sql-executor:
    encryption:
        secret-key: ${ENCRYPTION_SECRET_KEY:DefaultKey123456789012}
```

然后设置环境变量：

```bash
export ENCRYPTION_SECRET_KEY="YourProductionSecretKey"
```

### Docker 环境

在 `docker-compose.yml` 中配置：

```yaml
services:
    qoder-server:
        environment:
            - ENCRYPTION_SECRET_KEY=YourProductionSecretKey
```

## 🛡️ 安全建议

1. **强密钥**: 使用 32 位以上的随机字符串作为密钥
2. **密钥管理**:
    - 不要将密钥提交到版本控制系统
    - 使用密钥管理服务（如 AWS KMS, Azure Key Vault）
    - 定期轮换密钥（需要重新加密所有密码）
3. **访问控制**: 限制数据库访问权限
4. **审计日志**: 记录密码使用和修改操作
5. **传输安全**: 使用 HTTPS/TLS 保护网络传输

## ❓ 常见问题

### Q1: 修改密钥后无法连接数据库？

**A**: 密钥修改后，已加密的密码无法解密。需要：

1. 使用新密钥重新加密所有密码
2. 更新数据库中的密码字段

### Q2: 如何判断密码是否已加密？

**A**: 系统自动判断。加密密码特征：

-   Base64 格式
-   长度通常 > 50 个字符

可以查询数据库验证：

```sql
SELECT datasource_code,
       CASE
           WHEN LENGTH(password) > 50 THEN '已加密'
           ELSE '明文'
       END as status
FROM datasource_config;
```

### Q3: 可以混用明文和密文吗？

**A**: 可以。系统会自动识别：

-   明文密码：直接使用
-   密文密码：解密后使用

建议统一使用加密密码以提高安全性。

### Q4: 如何批量加密现有密码？

**A**: 编写脚本或 SQL 函数：

```sql
-- 1. 导出现有明文密码
SELECT datasource_code, password
FROM datasource_config
WHERE LENGTH(password) < 50;

-- 2. 使用加密工具逐个加密

-- 3. 批量更新
-- 使用 update-passwords-encrypted.sql 模板
```

## 📝 示例脚本

完整的密码更新脚本见：`update-passwords-encrypted.sql`

## 🔄 迁移现有系统

如果您已经有运行中的系统，迁移步骤：

1. 备份数据库
2. 配置加密密钥
3. 使用工具加密所有密码
4. 更新数据库
5. 重启应用
6. 验证所有数据源可正常连接

## 📞 技术支持

如有问题，请查看：

-   项目 README.md
-   系统日志
-   提交 Issue
