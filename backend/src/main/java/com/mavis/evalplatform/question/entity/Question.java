package com.mavis.evalplatform.question.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问题实体
 * <p>
 * 对齐架构设计说明书 §5.2.3 question 表 + FR-03 问题输入与管理
 * <p>
 * 由【向锏楠 FR-03】实现 Service / Controller。
 * <p>
 * 业务规则(对齐 §3.3.4):
 * <ul>
 *   <li>单题长度 ≤ 4000 字</li>
 *   <li>批量导入 ≤ 200 题/次</li>
 *   <li>软删除(保留历史评测引用)</li>
 * </ul>
 *
 * @author 向锏楠
 */
@Data
@TableName("question")
public class Question implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    /** 学科分类 */
    private String category;

    /** 简单/中等/困难 */
    private String difficulty;

    /** 事实/推理/创作/代码 */
    private String type;

    /** 期望答案(可选) */
    private String expectedAnswer;

    private Long createdBy;

    /** 软删除:0 正常 / 1 删除 */
    private Integer deleted;

    /** 是否公共题库:0 个人 / 1 公共 */
    private Integer isPublic;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
