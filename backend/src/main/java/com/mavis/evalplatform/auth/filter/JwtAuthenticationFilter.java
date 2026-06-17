package com.mavis.evalplatform.auth.filter;

import com.mavis.evalplatform.auth.entity.Role;
import com.mavis.evalplatform.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JWT 鉴权过滤器 — 实现骨架
 * <p>
 * 由【刘家豪 FR-01】完成实现。
 * <p>
 * 流程(对齐架构设计说明书 §4.2.1 关键流程第 9 步):
 * <ol>
 *   <li>从 Header {@code Authorization: Bearer <token>} 拿 token</li>
 *   <li>调用 {@code JwtService.isValid/parse} 解析</li>
 *   <li>把 uid/username/role 写到 {@code SecurityContextHolder}</li>
 *   <li>失败 → 清空 SecurityContext,继续执行(交给 SecurityConfig 拒绝)</li>
 * </ol>
 *
 * @author 刘家豪
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        // TODO 由刘家豪实现:解析 token,写 SecurityContext
        // 提示:用 UsernamePasswordAuthenticationToken,principal 用 AuthenticatedUser record
        // 失败时清空 SecurityContext,但不要 throw,让 Spring Security 的 authorize 拒绝
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 doFilterInternal");
    }

    /**
     * 已认证用户上下文(用于在 Controller 中读取 uid/role)
     * <p>
     * 其它模块也可在 Controller 用 {@code @AuthenticationPrincipal AuthenticatedUser user} 拿
     */
    public record AuthenticatedUser(Long id, String username, String role) {
        public boolean isAdmin() { return Role.ADMIN.getCode().equals(role); }
        public boolean isOrganizer() { return Role.ORGANIZER.getCode().equals(role) || isAdmin(); }
        public boolean isScorer() { return Role.SCORER.getCode().equals(role) || isOrganizer(); }
    }
}
