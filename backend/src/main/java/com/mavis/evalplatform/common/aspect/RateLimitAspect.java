package com.mavis.evalplatform.common.aspect;

import com.mavis.evalplatform.common.annotation.RateLimit;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.util.WebUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 限流切面 — 失败降级
 * <p>
 * Redis 不可用时,降级为放行(只记 WARN 日志,不阻塞业务)。
 * <p>
 * 算法:固定窗口计数器;key 格式: rl:{methodKey}:{ip}:{minute}
 *
 * @author 刘家豪
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String methodKey = rateLimit.byMethod() ? method.getDeclaringClass().getSimpleName() + "." + method.getName() : "global";
        String customKey = rateLimit.key();
        String ip = WebUtil.getClientIp();
        long minute = System.currentTimeMillis() / 60_000;
        String key = "rl:" + (customKey.isEmpty() ? methodKey : customKey) + ":" + ip + ":" + minute;

        int limit = rateLimit.limitPerMinute();
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(70));
            }
            if (count != null && count > limit) {
                log.warn("[RateLimit] key={}, ip={}, count={}, limit={}", key, ip, count, limit);
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
            }
        } catch (DataAccessException | BusinessException e) {
            if (e instanceof BusinessException) throw e;
            // Redis 不可用 → 降级放行,只 log
            log.warn("[RateLimit] Redis 不可用,降级放行: {}", e.getMessage());
        }
        return pjp.proceed();
    }
}
