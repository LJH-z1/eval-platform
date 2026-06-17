package com.mavis.evalplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录响应
 * <p>
 * 字段对齐架构设计说明书 §6.2.1
 *
 * @author 刘家豪
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "JWT Token(8 小时有效)")
    private String token;

    @Schema(description = "Token 类型")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "过期时间(秒)")
    private Long expiresIn;

    @Schema(description = "当前登录用户信息")
    private UserInfo userInfo;
}
