-- 数据源基本信息表
-- 用于存储多数据源配置信息

-- 数据源分类表
CREATE TABLE IF NOT EXISTS datasource_category (
    id SERIAL PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL UNIQUE,
    parent_id INTEGER,
    sort_order INTEGER DEFAULT 0,
    description VARCHAR(500),
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 数据源配置表
CREATE TABLE IF NOT EXISTS datasource_config (
    id SERIAL PRIMARY KEY,
    datasource_name VARCHAR(100) NOT NULL,
    datasource_code VARCHAR(50) NOT NULL UNIQUE,
    category_id INTEGER NOT NULL,
    db_type VARCHAR(50) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    jdbc_url VARCHAR(500),
    driver_class VARCHAR(200),
    is_enabled BOOLEAN DEFAULT TRUE,
    max_pool_size INTEGER DEFAULT 10,
    min_idle INTEGER DEFAULT 2,
    connection_timeout INTEGER DEFAULT 30000,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES datasource_category(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX idx_datasource_category_code ON datasource_category(category_code);
CREATE INDEX idx_datasource_category_parent ON datasource_category(parent_id);
CREATE INDEX idx_datasource_config_code ON datasource_config(datasource_code);
CREATE INDEX idx_datasource_config_category ON datasource_config(category_id);

-- 插入示例数据 - 分类
INSERT INTO datasource_category (category_name, category_code, parent_id, sort_order, description) VALUES
('生产环境', 'production', NULL, 1, '生产环境数据源'),
('测试环境', 'testing', NULL, 2, '测试环境数据源'),
('开发环境', 'development', NULL, 3, '开发环境数据源'),
('业务系统', 'business', 1, 1, '生产环境-业务系统'),
('数据仓库', 'warehouse', 1, 2, '生产环境-数据仓库'),
('日志系统', 'logging', 1, 3, '生产环境-日志系统');

-- 插入示例数据 - 数据源配置
-- 注意：实际使用时需要修改为真实的数据库连接信息
INSERT INTO datasource_config (
    datasource_name, datasource_code, category_id, db_type, 
    host, port, database_name, username, password, driver_class, description
) VALUES
('主业务数据库', 'main_business', 4, 'postgresql', 
 '192.168.1.6', 5433, 'mine', 'postgres', 'xxxx', 
 'org.postgresql.Driver', '主要业务系统数据库'),
 
('订单数据库', 'order_db', 4, 'postgresql', 
 '192.168.1.6', 5433, 'mine', 'postgres', 'xxxx', 
 'org.postgresql.Driver', '订单系统数据库'),
 
('用户数据库', 'user_db', 4, 'postgresql', 
 '192.168.1.6', 5433, 'mine', 'postgres', 'xxxx', 
 'org.postgresql.Driver', '用户中心数据库'),
 
('数据仓库', 'data_warehouse', 5, 'postgresql', 
 '192.168.1.6', 5433, 'mine', 'postgres', 'xxxx', 
 'org.postgresql.Driver', '数据仓库'),
 
('测试数据库', 'test_db', 2, 'postgresql', 
 '192.168.1.6', 5433, 'mine', 'postgres', 'xxxx', 
 'org.postgresql.Driver', '测试环境数据库'),
 
('开发数据库', 'dev_db', 3, 'postgresql', 
 '192.168.1.6', 5433, 'mine', 'postgres', 'xxxx', 
 'org.postgresql.Driver', '开发环境数据库');

-- 更新 jdbc_url
UPDATE datasource_config SET jdbc_url = 
    'jdbc:' || db_type || '://' || host || ':' || port || '/' || database_name;

-- 添加表注释
COMMENT ON TABLE datasource_category IS '数据源分类表';
COMMENT ON TABLE datasource_config IS '数据源配置表';

-- 添加字段注释 - datasource_category
COMMENT ON COLUMN datasource_category.id IS '主键ID';
COMMENT ON COLUMN datasource_category.category_name IS '分类名称';
COMMENT ON COLUMN datasource_category.category_code IS '分类编码';
COMMENT ON COLUMN datasource_category.parent_id IS '父级分类ID';
COMMENT ON COLUMN datasource_category.sort_order IS '排序顺序';
COMMENT ON COLUMN datasource_category.description IS '分类描述';
COMMENT ON COLUMN datasource_category.is_enabled IS '是否启用';
COMMENT ON COLUMN datasource_category.created_at IS '创建时间';
COMMENT ON COLUMN datasource_category.updated_at IS '更新时间';

-- 添加字段注释 - datasource_config
COMMENT ON COLUMN datasource_config.id IS '主键ID';
COMMENT ON COLUMN datasource_config.datasource_name IS '数据源名称';
COMMENT ON COLUMN datasource_config.datasource_code IS '数据源编码';
COMMENT ON COLUMN datasource_config.category_id IS '所属分类ID';
COMMENT ON COLUMN datasource_config.db_type IS '数据库类型 (postgresql, mysql, oracle 等)';
COMMENT ON COLUMN datasource_config.host IS '主机地址';
COMMENT ON COLUMN datasource_config.port IS '端口号';
COMMENT ON COLUMN datasource_config.database_name IS '数据库名称';
COMMENT ON COLUMN datasource_config.username IS '用户名';
COMMENT ON COLUMN datasource_config.password IS '密码（加密存储）';
COMMENT ON COLUMN datasource_config.jdbc_url IS 'JDBC连接URL（可选，自动生成）';
COMMENT ON COLUMN datasource_config.driver_class IS '驱动类名';
COMMENT ON COLUMN datasource_config.is_enabled IS '是否启用';
COMMENT ON COLUMN datasource_config.max_pool_size IS '最大连接池大小';
COMMENT ON COLUMN datasource_config.min_idle IS '最小空闲连接数';
COMMENT ON COLUMN datasource_config.connection_timeout IS '连接超时时间(毫秒)';
COMMENT ON COLUMN datasource_config.description IS '数据源描述';
COMMENT ON COLUMN datasource_config.created_at IS '创建时间';
COMMENT ON COLUMN datasource_config.updated_at IS '更新时间';

