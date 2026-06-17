package com.mavis.evalplatform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mavis.evalplatform.auth.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper — 接口骨架
 *
 * @author 刘家豪
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
    // TODO 由刘家豪实现具体 SQL
}
