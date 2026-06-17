package com.mavis.evalplatform.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesUtil 单元测试
 *
 * @author 刘家豪
 */
@DisplayName("AesUtil 单元测试")
class AesUtilTest {

    private AesUtil aesUtil;

    @BeforeEach
    void setUp() {
        aesUtil = new AesUtil();
        ReflectionTestUtils.setField(aesUtil, "secret", "0123456789abcdef01234567");
        aesUtil.init();
    }

    @Test
    @DisplayName("加密 → 解密 还原")
    void encrypt_decrypt_roundtrip() {
        String plain = "sk-1234567890abcdefABCDEF";
        String cipher = aesUtil.encrypt(plain);
        assertNotNull(cipher);
        assertNotEquals(plain, cipher);
        assertEquals(plain, aesUtil.decrypt(cipher));
    }

    @Test
    @DisplayName("空字符串加密后解密")
    void empty_string() {
        String cipher = aesUtil.encrypt("");
        assertEquals("", aesUtil.decrypt(cipher));
    }

    @Test
    @DisplayName("null 不抛异常")
    void null_input() {
        assertNull(aesUtil.encrypt(null));
        assertNull(aesUtil.decrypt(null));
    }

    @Test
    @DisplayName("日志脱敏 — 短串返回 ****")
    void mask_short() {
        assertEquals("****", AesUtil.mask("short"));
        assertEquals("****", AesUtil.mask(null));
    }

    @Test
    @DisplayName("日志脱敏 — 长串中间 ****")
    void mask_long() {
        String masked = AesUtil.mask("sk-1234567890ABCDEF");
        assertEquals("sk-1****CDEF", masked);
    }
}
