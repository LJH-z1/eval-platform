package com.mavis.evalplatform.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 服务 — 实现骨架
 * <p>
 * 由【刘家豪 FR-01】完成实现,其他人请勿修改。
 * <p>
 * 实现要点:
 * <ul>
 *   <li>HS256 签名,8 小时过期</li>
 *   <li>secret ≥ 32 字节,从配置 {@code eval.jwt.secret} 读取</li>
 *   <li>Claims:uid, username, role</li>
 *   <li>工具版本:jjwt 0.12.5(API 风格如 {@code Jwts.builder().claims(...).signWith(key, Jwts.SIG.HS256).compact()})</li>
 * </ul>
 *
 * @author 刘家豪
 */
@Slf4j
@Service
public class JwtService {

    @Value("${eval.jwt.secret}")
    private String secret;

    @Value("${eval.jwt.expire-hours:8}")
    private long expireHours;

    private SecretKey key;
    private long expireSeconds;

    @PostConstruct
    public void init() {
        // TODO 由刘家豪实现:校验 secret ≥ 32 字节,初始化 key 与 expireSeconds
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 JwtService.init()");
    }

    public String generateToken(Long userId, String username, String role) {
        // TODO 由刘家豪实现:签发 HS256 JWT,Claims 含 uid/username/role
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 generateToken");
    }

    public Claims parse(String token) {
        // TODO 由刘家豪实现:解析 token,失败抛 JwtException
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 parse");
    }

    public boolean isValid(String token) {
        // TODO 由刘家豪实现:不抛异常的校验
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 isValid");
    }

    public long getExpireSeconds() {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 getExpireSeconds");
    }

    public boolean isExpired(String token) {
        // TODO 由刘家豪实现
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 isExpired");
    }
}
