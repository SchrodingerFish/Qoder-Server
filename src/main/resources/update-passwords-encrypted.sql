-- ============================================
-- 更新数据源密码为加密格式
-- ============================================
-- 
-- 使用说明：
-- 1. 运行 PasswordEncryptionTool 工具加密您的密码
-- 2. 将下面的 '<加密后的密码>' 替换为加密结果
-- 3. 执行此脚本更新数据库
--
-- 加密工具使用：
--   cd Qoder-Server
--   mvn spring-boot:run -Dstart-class=com.example.sqlexecutor.util.PasswordEncryptionTool -Dspring-boot.run.arguments="encrypt Qcj8813818@#"
--

-- 示例：更新所有数据源的密码（请根据实际情况修改）
-- 假设您的密码 'Qcj8813818@#' 加密后为某个Base64字符串

-- 方法1: 逐个更新
UPDATE datasource_config SET password = '<加密后的密码>' WHERE datasource_code = 'main_business';
UPDATE datasource_config SET password = '<加密后的密码>' WHERE datasource_code = 'order_db';
UPDATE datasource_config SET password = '<加密后的密码>' WHERE datasource_code = 'user_db';
UPDATE datasource_config SET password = '<加密后的密码>' WHERE datasource_code = 'data_warehouse';
UPDATE datasource_config SET password = '<加密后的密码>' WHERE datasource_code = 'test_db';
UPDATE datasource_config SET password = '<加密后的密码>' WHERE datasource_code = 'dev_db';

-- 方法2: 如果所有数据源使用相同密码，可以批量更新
-- UPDATE datasource_config SET password = '<加密后的密码>';

-- 验证更新结果
SELECT datasource_code, datasource_name, 
       CASE 
           WHEN LENGTH(password) > 50 THEN '已加密 (' || LENGTH(password) || ' 字符)'
           ELSE '明文密码'
       END as password_status
FROM datasource_config
ORDER BY id;

