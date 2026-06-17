package com.mavis.evalplatform.common.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES 加密工具
 * <p>
 * 用于 model_config.api_key 字段的加密存储(对齐架构设计说明书 §8.3)。
 * <p>
 * 注意:
 * 1. 密钥必须为 16/24/32 字节(对应 AES-128/192/256)
 * 2. 实际生产环境密钥从环境变量 {@code ENCRYPTION_KEY} 注入
 *
 * @author 刘家豪
 */
@Slf4j
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${eval.encryption.api-key-secret}")
    private String secret;

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(secret) || secret.length() < 16) {
            throw new IllegalStateException(
                    "eval.encryption.api-key-secret 必须 ≥ 16 字节,当前: " + secret);
        }
        byte[] keyBytes = padOrTruncate(secret.getBytes(StandardCharsets.UTF_8), 16);
        this.keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        log.info("[AesUtil] 初始化完成,算法={}, key_len={}", ALGORITHM, keyBytes.length);
    }

    /**
     * 加密(返回 Base64 字符串)
     */
    public String encrypt(String plain) {
        if (plain == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    /**
     * 解密
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] dec = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES 解密失败(可能密钥不匹配)", e);
        }
    }

    /**
     * 日志脱敏(显示前 4 后 4,中间 ****)
     */
    public static String mask(String key) {
        if (key == null || key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private static byte[] padOrTruncate(byte[] src, int target) {
        if (src.length == target) return src;
        byte[] dst = new byte[target];
        if (src.length > target) {
            System.arraycopy(src, 0, dst, 0, target);
        } else {
            System.arraycopy(src, 0, dst, 0, src.length);
            // 不足部分补 0
        }
        return dst;
    }
}
