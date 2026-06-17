package com.mavis.evalplatform.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改密码请求
 *
 * @author 刘家豪
 */
@Data
@Schema(description = "修改密码")
public class ChangePasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 20, message = "长度需为 6-20 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "至少包含字母与数字")
    private String newPassword;
}
