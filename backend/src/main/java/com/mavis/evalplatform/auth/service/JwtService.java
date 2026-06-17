package com.mavis.evalplatform.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
 * JWT 服务 — 最小可用实现(让项目能起来)
 * <p>
 * HS256 签名,8 小时过期
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
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "eval.jwt.secret 必须 ≥ 32 字节(HS256 要求),当前: " + bytes.length);
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireSeconds = expireHours * 3600L;
        log.info("[JwtService] 初始化完成,expire={}h", expireHours);
    }

    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("username", username);
        claims.put("role", role);
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000L);
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isValid(String token) {
        try { parse(token); return true; } catch (Exception e) { return false; }
    }

    public long getExpireSeconds() { return expireSeconds; }

    public boolean isExpired(String token) {
        try { return parse(token).getExpiration().toInstant().isBefore(Instant.now()); }
        catch (JwtException e) { return true; }
    }
}
