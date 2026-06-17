package com.mavis.evalplatform.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型配置展示 VO(API Key 脱敏输出)。
 * <p>
 * 对齐 FR-02 §3.2.4:列表/详情接口中 API Key 必须掩码,不允许明文出库。
 * <p>
 * 由 {@code ModelService} 负责把 {@code ModelConfig} 实体 + 解密后的明文 API Key 转为 VO。
 *
 * @author 向锏楠
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "模型配置 VO")
public class ModelConfigVO {

    private Long id;
    private String name;
    private String provider;
    /** 掩码后的 API Key,格式:前 4 + **** + 后 4 */
    private String apiKeyMasked;
    private String endpoint;
    private String modelVersion;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer maxTokens;
    private BigDecimal pricePerK;
    /** 1 启用 / 0 禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
