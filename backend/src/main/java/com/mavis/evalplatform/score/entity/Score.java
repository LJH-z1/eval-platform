package com.mavis.evalplatform.score.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评分实体
 * <p>
 * 对齐架构设计说明书 §5.2.6 score 表 + FR-05 多维评分
 * <p>
 * 由【宋子翔 FR-05】实现。
 * <p>
 * 4 个维度(1-5 分):
 * <ul>
 *   <li>accuracy 准确性</li>
 *   <li>relevance 相关性</li>
 *   <li>fluency 流畅性</li>
 *   <li>safety 安全性</li>
 * </ul>
 * <p>
 * 唯一约束:UNIQUE(answer_id, scorer_id) — 同一评分员对同一回答只能评 1 次
 *
 * @author 宋子翔
 */
@Data
@TableName("score")
public class Score implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long answerId;
    private Long scorerId;

    private Integer accuracy;
    private Integer relevance;
    private Integer fluency;
    private Integer safety;

    private String comment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
