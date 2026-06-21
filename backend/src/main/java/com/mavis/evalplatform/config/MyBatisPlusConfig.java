package com.mavis.evalplatform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * <p>
 * - 启用分页插件
 * - 启用 createdAt / updatedAt / createdBy 自动填充
 *
 * @author 刘家豪
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt",   LocalDateTime.class, now);
                strictInsertFill(metaObject, "createTime",  LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt",   LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime",  LocalDateTime.class, now);
                strictInsertFill(metaObject, "created_at",  LocalDateTime.class, now);
                strictInsertFill(metaObject, "updated_at",  LocalDateTime.class, now);
                // 从 SecurityContext 拿当前登录用户
                Long uid = currentUserId();
                if (uid != null) {
                    strictInsertFill(metaObject, "createdBy", Long.class, uid);
                }
            }
            @Override
            public void updateFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictUpdateFill(metaObject, "updatedAt",   LocalDateTime.class, now);
                strictUpdateFill(metaObject, "updateTime",  LocalDateTime.class, now);
                strictUpdateFill(metaObject, "updated_at",  LocalDateTime.class, now);
            }
        };
    }

    private Long currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            if (principal == null) return null;
            // JwtAuthenticationFilter.AuthenticatedUser record: id(username, role)
            // 用反射拿 id 字段,避免硬依赖
            try {
                Object id = principal.getClass().getMethod("id").invoke(principal);
                if (id instanceof Number) return ((Number) id).longValue();
            } catch (Exception ignore) {}
            if (principal instanceof Long) return (Long) principal;
            if (principal instanceof Number) return ((Number) principal).longValue();
            return Long.parseLong(principal.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
