package com.mavis.evalplatform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * 对齐架构设计说明书 §5.2.1 user 表
 * <p>
 * 由【刘家豪 FR-01】实现,本类为字段定义骨架(不包含业务方法)。
 *
 * @author 刘家豪
 */
@Data
@TableName("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 哈希 */
    private String password;

    private String email;

    /** 角色:ADMIN/ORGANIZER/SCORER/VISITOR */
    private String role;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    /** 连续登录失败计数,达到 5 触发锁定 */
    private Integer failedCount;

    /** 锁定截止时间(锁定 30 分钟) */
    private LocalDateTime lockedUntil;

    @TableField(value = "created_at", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
