package com.mavis.evalplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户信息(返回给前端的精简版,不包含 password)
 *
 * @author 刘家豪
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "角色描述")
    private String roleDescription;

    @Schema(description = "状态 1启用 0禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
