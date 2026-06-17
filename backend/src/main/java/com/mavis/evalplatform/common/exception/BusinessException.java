package com.mavis.evalplatform.common.exception;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 业务层抛出此异常后,由 {@code GlobalExceptionHandler} 统一转换为 {@code Result} 响应。
 *
 * @author 刘家豪
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.OPERATION_FAILED.getCode();
    }
}
