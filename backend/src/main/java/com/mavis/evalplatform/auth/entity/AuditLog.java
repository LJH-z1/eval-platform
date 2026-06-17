package com.mavis.evalplatform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 * <p>
 * 对齐架构设计说明书 §5.2.7 audit_log 表 + FR-01.06 操作日志
 *
 * @author 刘家豪
 */
@Data
@TableName("audit_log")
public class AuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;

    /** LOGIN / LOGIN_FAIL / LOGOUT / CREATE_USER / SCORE / EXPORT 等 */
    private String action;

    /** 目标资源(如 evaluationId=1) */
    private String target;

    private String ip;

    /** SUCCESS / FAIL */
    private String status;

    private String detail;

    private LocalDateTime createdAt;
}
