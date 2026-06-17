package com.mavis.evalplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册/创建用户请求
 * <p>
 * 管理员可调用 {@code POST /api/auth/register} 创建账号
 * <p>
 * 业务规则(对齐 §3.1.5 需求规格说明书):
 * - 用户名 3-20 位
 * - 密码 6-20 位,至少含字母与数字
 *
 * @author 刘家豪
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "不能为空")
    @Size(min = 3, max = 20, message = "长度需为 3-20 位")
    @Schema(description = "用户名", example = "scorer1")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 20, message = "长度需为 6-20 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "至少包含字母与数字")
    @Schema(description = "密码", example = "Test123456")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "scorer1@eval.local")
    private String email;

    @NotBlank(message = "不能为空")
    @Schema(description = "角色:ADMIN/ORGANIZER/SCORER/VISITOR", example = "SCORER")
    private String role;
}
