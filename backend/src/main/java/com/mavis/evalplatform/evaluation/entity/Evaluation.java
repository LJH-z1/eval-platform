package com.mavis.evalplatform.evaluation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评测实体
 * <p>
 * 对齐架构设计说明书 §5.2.4 evaluation 表 + FR-04 多模型调用
 * <p>
 * 由【梁倩倩 FR-04】实现。
 *
 * @author 梁倩倩
 */
@Data
@TableName("evaluation")
public class Evaluation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private Long createdBy;

    /** PENDING/RUNNING/COMPLETED/FAILED */
    private String status;

    /** 逗号分隔的模型 ID */
    private String modelIds;

    /** 逗号分隔的问题 ID */
    private String questionIds;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
