package com.mavis.evalplatform.auth.entity;

import lombok.Getter;

/**
 * 角色枚举
 * <p>
 * 对齐需求规格说明书 §2.2 / §3.1.3 — 4 个内置角色
 *
 * @author 刘家豪
 */
@Getter
public enum Role {

    /** 系统管理员:全权限 */
    ADMIN("ADMIN", "系统管理员"),
    /** 评测组织者:创建评测、邀请评分员、导出报告 */
    ORGANIZER("ORGANIZER", "评测组织者"),
    /** 评分员:对回答打分 */
    SCORER("SCORER", "评分员"),
    /** 普通访客:只读 */
    VISITOR("VISITOR", "普通访客");

    private final String code;
    private final String description;

    Role(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Role of(String code) {
        if (code == null) return VISITOR;
        for (Role r : values()) {
            if (r.code.equalsIgnoreCase(code)) return r;
        }
        return VISITOR;
    }
}
