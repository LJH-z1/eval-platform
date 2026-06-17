package com.mavis.evalplatform.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型配置创建/更新请求
 * <p>
 * 用于 {@code POST /api/models} 和 {@code PUT /api/models/{id}}。
 * <p>
 * 业务规则(对齐 FR-02 §3.2.4):
 * <ul>
 *   <li>{@code apiKey} 明文传入,服务端 AES-256 加密后存储</li>
 *   <li>更新时 {@code provider} 不允许修改(由 Controller 校验)</li>
 *   <li>name 唯一</li>
 * </ul>
 *
 * @author 向锏楠
 */
@Data
@Schema(description = "模型配置请求")
public class ModelConfigRequest {

    @Schema(description = "用户自定义名称(唯一)", example = "M3-prod")
    @NotBlank(message = "模型名称不能为空")
    @Size(min = 1, max = 50, message = "模型名称长度需在 1-50 之间")
    private String name;

    @Schema(description = "提供商(M3/ZHIPU/QWEN/WENXIN/KIMI)", example = "M3")
    @NotBlank(message = "提供商不能为空")
    private String provider;

    @Schema(description = "明文 API Key,服务端 AES-256 加密后存储")
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;

    @Schema(description = "模型 API endpoint", example = "https://api.MiniMax.chat/v1/text/chatcompletion_v2")
    private String endpoint;

    @Schema(description = "模型版本", example = "M3-Plus")
    private String modelVersion;

    @Schema(description = "温度 0-1,默认 0.7")
    private BigDecimal temperature;

    @Schema(description = "Top-P 0-1,默认 0.9")
    private BigDecimal topP;

    @Schema(description = "最大输出 Token,默认 2048")
    private Integer maxTokens;

    @Schema(description = "每千 Token 单价(元)")
    private BigDecimal pricePerK;

    @Schema(description = "状态:1 启用 / 0 禁用,默认 1")
    private Integer status;

    /**
     * 给默认值,避免 NPE
     */
    public void applyDefaults() {
        if (temperature == null) temperature = new BigDecimal("0.7");
        if (topP == null) topP = new BigDecimal("0.9");
        if (maxTokens == null) maxTokens = 2048;
        if (status == null) status = 1;
    }
}
