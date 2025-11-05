package com.example.sqlexecutor.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 密码加密工具
 * 
 * 使用方法：
 * 1. 运行此类的 main 方法
 * 2. 输入需要加密的密码
 * 3. 获得加密后的密码，更新到数据库中
 * 
 * 或者使用命令行：
 * mvn spring-boot:run -Dspring-boot.run.arguments="encrypt 123456"
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.example.sqlexecutor.util")
public class PasswordEncryptionTool {

    public static void main(String[] args) {
        SpringApplication.run(PasswordEncryptionTool.class, args);
    }

    @Bean
    CommandLineRunner run(PasswordEncryptor encryptor) {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("密码加密/解密工具");
            System.out.println("========================================\n");

            if (args.length == 0) {
                System.out.println("使用方法:");
                System.out.println("  加密: java -jar xxx.jar encrypt <明文密码>");
                System.out.println("  解密: java -jar xxx.jar decrypt <加密密码>");
                System.out.println("\n示例:");
                System.out.println("  java -jar xxx.jar encrypt 123456");
                System.out.println("\n或者在代码中直接修改下面的密码进行测试:\n");

                // 示例密码
                String[] testPasswords = {
                        "123456",
                        "7890abC!",
                        "password123",
                        "test@123"
                };

                System.out.println("加密示例:");
                for (String password : testPasswords) {
                    String encrypted = encryptor.encrypt(password);
                    System.out.println("  明文: " + password);
                    System.out.println("  密文: " + encrypted);
                    System.out.println("  验证: " + encryptor.decrypt(encrypted));
                    System.out.println();
                }

                System.out.println("\n生成随机密钥:");
                System.out.println("  " + PasswordEncryptor.generateRandomKey());

            } else if (args.length == 2) {
                String command = args[0].toLowerCase();
                String password = args[1];

                if ("encrypt".equals(command)) {
                    String encrypted = encryptor.encrypt(password);
                    System.out.println("原始密码: " + password);
                    System.out.println("加密密码: " + encrypted);
                    System.out.println("\nSQL更新语句示例:");
                    System.out.println("UPDATE datasource_config SET password = '" + encrypted
                            + "' WHERE datasource_code = 'your_code';");

                } else if ("decrypt".equals(command)) {
                    String decrypted = encryptor.decrypt(password);
                    System.out.println("加密密码: " + password);
                    System.out.println("解密密码: " + decrypted);

                } else {
                    System.out.println("未知命令: " + command);
                    System.out.println("支持的命令: encrypt, decrypt");
                }
            } else {
                System.out.println("参数错误!");
                System.out.println("使用方法: <command> <password>");
            }

            System.out.println("\n========================================\n");
        };
    }
}
