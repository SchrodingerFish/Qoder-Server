package com.example.sqlexecutor;

import com.example.sqlexecutor.util.PasswordEncryptionTool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SQL执行器应用主启动类
 */
@SpringBootApplication
public class SqlExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlExecutorApplication.class, args);
    }
}