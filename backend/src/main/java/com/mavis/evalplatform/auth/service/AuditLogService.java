package com.mavis.evalplatform.auth.service;

import com.mavis.evalplatform.auth.entity.AuditLog;
import com.mavis.evalplatform.auth.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 审计日志服务 — 实现骨架
 * <p>
 * 由【刘家豪 FR-01】完成实现,其他人请勿修改。
 *
 * @author 刘家豪
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 异步记录审计日志
     * <p>
     * 用 @Async("auditExecutor")(在 config/ExecutorConfig.java 中定义)
     */
    @Async("auditExecutor")
    public void logAsync(Long userId, String username, String action,
                         String target, String ip, String status, String detail) {
        // TODO 由刘家豪实现:写 audit_log 表
        // 注意:写入失败不应影响主业务(try-catch 后仅 log)
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 logAsync");
    }
}
