package com.mavis.evalplatform.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 答案实体
 * <p>
 * 对齐架构设计说明书 §5.2.5 answer 表
 * <p>
 * 由【梁倩倩 FR-04】实现。
 *
 * @author 梁倩倩
 */
@Data
@TableName("answer")
public class Answer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long evaluationId;
    private Long questionId;
    private Long modelId;

    private String content;

    /** 响应耗时(毫秒) */
    private Integer latencyMs;

    private Integer tokenInput;
    private Integer tokenOutput;

    /** 估算费用(元) */
    private BigDecimal estimatedCost;

    /** 错误码(成功为 null) */
    private String errorCode;
    private String errorMessage;

    private LocalDateTime createdAt;
}
