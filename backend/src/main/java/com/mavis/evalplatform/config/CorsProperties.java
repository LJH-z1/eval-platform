package com.mavis.evalplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * CORS 配置属性
 *
 * @author 刘家豪
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "eval.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
