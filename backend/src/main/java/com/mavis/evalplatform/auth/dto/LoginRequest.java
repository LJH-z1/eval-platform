package com.mavis.evalplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求
 *
 * @author 刘家豪
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "不能为空")
    @Size(min = 3, max = 20, message = "长度需为 3-20 位")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 20, message = "长度需为 6-20 位")
    @Schema(description = "密码", example = "admin123")
    private String password;
}
