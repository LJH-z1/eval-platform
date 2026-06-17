package com.mavis.evalplatform.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * auth 子包配置占位
 * <p>
 * PasswordEncoder、JwtService 等 Bean 由 {@code config.SecurityConfig} / 各 {@code @Service} 自行管理,
 * 此处保留空 Configuration 用于未来扩展(例如 {@code @ConfigurationProperties} 绑定)。
 *
 * @author 刘家豪
 */
@Configuration
@EnableConfigurationProperties
public class AuthBeansConfig {
}
