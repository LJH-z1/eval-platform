package com.mavis.evalplatform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型连接测试请求
 * <p>
 * 用于 {@code POST /api/models/test}。
 *
 * @author 向锏楠
 */
@Data
@Schema(description = "模型连接测试请求")
public class ModelTestRequest {

    @Schema(description = "模型 ID")
    private Long id;

    @Schema(description = "提供商(与 ID 二选一)", example = "M3")
    private String provider;

    @Schema(description = "测试问题", example = "你好")
    private String apiKey;

    @Schema(description = "endpoint(可选)", example = "https://api.example.com/v1/chat")
    private String endpoint;

    @Schema(description = "模型版本(可选)", example = "M3-Plus")
    private String modelVersion;

    @Schema(description = "温度(可选,默认 0.7)")
    private java.math.BigDecimal temperature;

    @Schema(description = "Top-P(可选,默认 0.9)")
    private java.math.BigDecimal topP;

    @Schema(description = "最大 Token(可选,默认 512)")
    private Integer maxTokens;

    @Schema(description = "测试问题文本", example = "你好,请用一句话介绍下你自己。")
    @NotBlank(message = "测试问题不能为空")
    private String question;
}
