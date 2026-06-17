package com.mavis.evalplatform.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 统一响应包装
 * <p>
 * 对齐架构设计说明书 §6.1 统一响应格式
 * {code, message, data, timestamp}
 *
 * @param <T> 数据类型
 * @author 刘家豪
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码,200 表示成功")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "服务器时间戳(毫秒)")
    private Long timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "ok";
        r.data = data;
        r.timestamp = Instant.now().toEpochMilli();
        return r;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = message;
        r.data = data;
        r.timestamp = Instant.now().toEpochMilli();
        return r;
    }

    public static <T> Result<T> error(Integer code, String message) {
        return error(code, message, null);
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = data;
        r.timestamp = Instant.now().toEpochMilli();
        return r;
    }
}
