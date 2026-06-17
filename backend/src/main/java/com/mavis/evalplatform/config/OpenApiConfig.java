package com.mavis.evalplatform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 3 配置
 * <p>
 * - UI 路径:/swagger-ui.html
 * - JSON 路径:/v3/api-docs
 * - 接入 JWT 鉴权(对接架构设计说明书 §1.4.4 接口约定)
 *
 * @author 刘家豪
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("多模型回答对比与评测平台 API")
                        .description("FR-01 用户登录与权限管理 / FR-02~08 各模块接口\n" +
                                "项目组:软件测试小组 | 后端主程:刘家豪")
                        .version("1.0.0")
                        .contact(new Contact().name("刘家豪").email("liujh@eval-platform.local"))
                        .license(new License().name("Course Project").url("https://github.com/eval-platform-team")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("在请求头 Authorization: Bearer <token> 中携带 JWT")));
    }
}
