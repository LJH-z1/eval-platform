package com.mavis.evalplatform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型配置实体
 * <p>
 * 对齐架构设计说明书 §5.2.2 model_config 表 + FR-02 模型配置管理
 * <p>
 * 由【向锏楠 FR-02】实现 Service / Controller / SQL。
 * <p>
 * 关键约束:
 * <ul>
 *   <li>{@code apiKey} 必须 AES-256 加密后存储(用 {@code common.util.AesUtil})</li>
 *   <li>{@code provider} 取值:M3 / ZHIPU / QWEN / WENXIN / KIMI</li>
 *   <li>同一提供商可配置多个模型版本</li>
 *   <li>被评测引用的模型不允许删除(FR-02.04 业务规则)</li>
 * </ul>
 *
 * @author 向锏楠
 */
@Data
@TableName("model_config")
public class ModelConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户自定义名,UNIQUE */
    private String name;

    /** 提供商:M3/ZHIPU/QWEN/WENXIN/KIMI */
    private String provider;

    /** AES-256 加密后的 API Key */
    private String apiKey;

    /** 模型 API endpoint */
    private String endpoint;

    /** 模型版本(如 "M3", "glm-4-Plus", "qwen-max") */
    private String modelVersion;

    /** 温度,默认 0.7 */
    private BigDecimal temperature;

    /** Top-P,默认 0.9 */
    private BigDecimal topP;

    /** 最大输出 Token,默认 2048 */
    private Integer maxTokens;

    /** 每千 Token 单价(元) */
    private BigDecimal pricePerK;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
