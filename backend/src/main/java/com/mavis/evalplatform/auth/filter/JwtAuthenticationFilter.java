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
 * JWT 鉴权过滤器 — 最小可用实现
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
        try {
            String header = req.getHeader(HEADER);
            if (StringUtils.hasText(header) && header.startsWith(PREFIX)) {
                String token = header.substring(PREFIX.length()).trim();
                if (jwtService.isValid(token)) {
                    Claims claims = jwtService.parse(token);
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    Long uid = claims.get("uid", Long.class);

                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                new AuthenticatedUser(uid, username, role),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        } catch (JwtException e) {
            log.debug("[JWT] 解析失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(req, res);
    }

    public record AuthenticatedUser(Long id, String username, String role) {
        public boolean isAdmin() { return Role.ADMIN.getCode().equals(role); }
    }
}
