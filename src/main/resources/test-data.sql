-- 创建测试数据库和表
-- 注意：在实际使用前需要先创建数据库和表

CREATE TABLE "public"."users" (
"id" serial NOT NULL,
"username" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
"email" varchar(255) COLLATE "pg_catalog"."default",
"password" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
"age" int4,
"is_admin" int4 DEFAULT 0,
"address" varchar(200) COLLATE "pg_catalog"."default",
"introduction" text COLLATE "pg_catalog"."default",
"sex" int4,
"phone" varchar(11) COLLATE "pg_catalog"."default" NOT NULL,
CONSTRAINT "users_pkey" PRIMARY KEY ("id"),
CONSTRAINT "users_email_key" UNIQUE ("email"),
CONSTRAINT "users_phone_key" UNIQUE ("phone"),
CONSTRAINT "users_age_check" CHECK (age >= 0 AND age <= 150),
CONSTRAINT "chk_users_is_admin" CHECK (is_admin = ANY (ARRAY[0, 1]))
)
;

ALTER TABLE "public"."users"
    OWNER TO "postgres";

CREATE INDEX "idx_users_email" ON "public"."users" USING btree (
    "email" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

CREATE INDEX "idx_users_phone" ON "public"."users" USING btree (
    "phone" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

CREATE INDEX "idx_users_username" ON "public"."users" USING btree (
    "username" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
    );

COMMENT ON COLUMN "public"."users"."username" IS '用户名';

COMMENT ON COLUMN "public"."users"."email" IS '邮箱';

COMMENT ON COLUMN "public"."users"."password" IS '密码';

COMMENT ON COLUMN "public"."users"."age" IS '年龄';

COMMENT ON COLUMN "public"."users"."is_admin" IS '是否为超级管理员(0否1是)';

COMMENT ON COLUMN "public"."users"."address" IS '地址';

COMMENT ON COLUMN "public"."users"."introduction" IS '简介';

COMMENT ON COLUMN "public"."users"."sex" IS '性别(0女1男)';

COMMENT ON COLUMN "public"."users"."phone" IS '手机号';

COMMENT ON TABLE "public"."users" IS '用户表';-- SELECT u.username, p.name, p.price FROM users u CROSS JOIN products p LIMIT 10;