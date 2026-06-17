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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 限流切面
 * <p>
 * 算法:固定窗口计数器
 * key 格式: rl:{methodKey}:{ip}:{minute}
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

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(70));
        }
        int limit = rateLimit.limitPerMinute();
        if (count != null && count > limit) {
            log.warn("[RateLimit] key={}, ip={}, count={}, limit={}", key, ip, count, limit);
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        return pjp.proceed();
    }
}
