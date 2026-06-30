package com.mavis.evalplatform.arena.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Arena 盲评投票 — 存储用户对两个模型的偏好
 *
 * @author 刘家豪
 */
@Data
@TableName("arena_vote")
public class ArenaVote implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long evaluationId;
    private Long voterId;
    private String prompt;
    private Long leftModelId;
    private Long rightModelId;

    /** A / B / tie / bad */
    private String winner;

    /** 能力分类:text / code / writing / math / vision / general 等 */
    private String category;

    private LocalDateTime createdAt;
}
