package com.mavis.evalplatform.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * Web 工具
 *
 * @author 刘家豪
 */
public final class WebUtil {

    private WebUtil() {}

    /** 获取当前请求 IP */
    public static String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest req = attrs.getRequest();
        String ip = req.getHeader("X-Forwarded-For");
        if (isInvalidIp(ip)) ip = req.getHeader("X-Real-IP");
        if (isInvalidIp(ip)) ip = req.getHeader("Proxy-Client-IP");
        if (isInvalidIp(ip)) ip = req.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return Objects.requireNonNullElse(ip, "unknown");
    }

    private static boolean isInvalidIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }
}
