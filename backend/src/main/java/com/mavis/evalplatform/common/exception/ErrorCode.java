package com.mavis.evalplatform.common.exception;

import lombok.Getter;

/**
 * 业务错误码枚举
 * <p>
 * 范围:1001-1099 业务错误(对齐架构设计说明书 §6.1)
 * 其他:见 {@link #code} 注释
 *
 * @author 刘家豪
 */
@Getter
public enum ErrorCode {

    // 通用
    SUCCESS(200, "ok"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁,请稍后再试"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 业务 1001-1099
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已被占用"),
    PASSWORD_INCORRECT(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用"),
    USER_LOCKED(1005, "账号已被锁定,请 30 分钟后再试"),
    INVALID_USERNAME_FORMAT(1006, "用户名长度需为 3-20 位"),
    INVALID_PASSWORD_FORMAT(1007, "密码长度需为 6-20 位,且至少包含字母与数字"),
    TOKEN_EXPIRED(1008, "登录已过期,请重新登录"),
    TOKEN_INVALID(1009, "无效的令牌"),
    OLD_PASSWORD_INCORRECT(1010, "原密码错误"),
    ROLE_NOT_FOUND(1011, "角色不存在"),

    // 通用业务
    DATA_NOT_FOUND(1020, "数据不存在"),
    OPERATION_FAILED(1021, "操作失败"),
    PARAM_INVALID(1022, "参数校验失败"),

    // 模型相关(FR-02)
    MODEL_REFERENCED(1023, "模型已被评测引用,无法删除"),
    MODEL_NOT_FOUND(1024, "模型不存在"),
    MODEL_API_KEY_INVALID(1025, "API Key 无效"),
    MODEL_TEST_FAILED(1026, "模型连接测试失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
