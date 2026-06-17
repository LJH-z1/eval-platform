package com.mavis.evalplatform.auth.service;

import com.mavis.evalplatform.auth.entity.AuditLog;
import com.mavis.evalplatform.auth.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志 — 最小可用实现
 *
 * @author 刘家豪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public void logAsync(Long userId, String username, String action, String target,
                         String ip, String status, String detail) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction(action);
            log.setTarget(target);
            log.setIp(ip);
            log.setStatus(status);
            log.setDetail(detail);
            log.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            // 失败不应当影响主业务
            log.error("[AuditLog] 写入失败 action={}, user={}", action, username, e);
        }
    }
}
