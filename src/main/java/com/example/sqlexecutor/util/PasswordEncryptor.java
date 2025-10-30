package com.example.sqlexecutor.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 密码加密解密工具类
 * 使用 AES-256-GCM 加密算法
 */
@Slf4j
@Component
public class PasswordEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${sql-executor.encryption.secret-key:MySecretKey123456MySecretKey123456}")
    private String secretKeyString;

    /**
     * 加密密码
     */
    public String encrypt(String plainText) {
        try {
            SecretKey secretKey = getSecretKey();

            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 加密
            byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将 IV 和加密数据组合
            byte[] combined = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedData, 0, combined, iv.length, encryptedData.length);

            // Base64 编码
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new RuntimeException("密码加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密密码
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKey secretKey = getSecretKey();

            // Base64 解码
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // 分离 IV 和加密数据
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedData = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encryptedData, 0, encryptedData.length);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 解密
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("密码解密失败", e);
            throw new RuntimeException("密码解密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断密码是否已加密
     * 简单判断：如果是 Base64 格式且长度大于一定值，认为是加密的
     */
    public boolean isEncrypted(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }

        try {
            // 尝试 Base64 解码
            byte[] decoded = Base64.getDecoder().decode(password);
            // 加密后的密码应该至少包含 IV (12字节) + 一些数据
            return decoded.length > GCM_IV_LENGTH;
        } catch (IllegalArgumentException e) {
            // 不是有效的 Base64，说明是明文
            return false;
        }
    }

    /**
     * 获取密钥
     */
    private SecretKey getSecretKey() throws Exception {
        // 使用配置的密钥字符串生成固定的 AES 密钥
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(secretKeyString.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }

    /**
     * 生成随机密钥（用于初始化配置）
     */
    public static String generateRandomKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成密钥失败", e);
        }
    }
}
