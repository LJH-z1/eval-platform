package com.mavis.evalplatform.common.exception;

import com.mavis.evalplatform.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 * <p>
 * 把所有异常统一转换为 {@link Result} 格式,前端只关心 code/message/data。
 * 对齐架构设计说明书 §6.1 统一响应格式。
 *
 * @author 刘家豪
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("[BusinessException] {} {} -> code={}, msg={}",
                req.getMethod(), req.getRequestURI(), ex.getCode(), ex.getMessage());
        return ResponseEntity.ok(Result.error(ex.getCode(), ex.getMessage()));
    }

    /** @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("[Validation] {}", msg);
        return ResponseEntity.ok(Result.error(ErrorCode.PARAM_INVALID.getCode(), msg));
    }

    /** 表单绑定错误 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.ok(Result.error(ErrorCode.PARAM_INVALID.getCode(), msg));
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.ok(Result.error(ErrorCode.BAD_REQUEST.getCode(),
                "缺少必填参数:" + ex.getParameterName()));
    }

    /** Spring Security:凭证错误(用户名/密码错) */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Result<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.ok(Result.error(ErrorCode.PASSWORD_INCORRECT.getCode(),
                ErrorCode.PASSWORD_INCORRECT.getMessage()));
    }

    /** Spring Security:未登录 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(ErrorCode.UNAUTHORIZED.getCode(), ex.getMessage()));
    }

    /** Spring Security:无权限 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(ErrorCode.FORBIDDEN.getCode(), ex.getMessage()));
    }

    /** 兜底:未预期异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAll(Exception ex, HttpServletRequest req) {
        log.error("[UnhandledException] {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.INTERNAL_ERROR.getCode(),
                        "服务器开了小差,请稍后再试"));
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + " " + fe.getDefaultMessage();
    }
}
