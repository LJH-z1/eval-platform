package com.mavis.evalplatform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mavis.evalplatform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * User Mapper — 接口骨架
 * <p>
 * 由【刘家豪 FR-01】实现具体 SQL。
 * <p>
 * 其他模块可能用到的 SQL:
 * <ul>
 *   <li>{@code selectByUsername} — AuthService.login 用</li>
 *   <li>{@code incrementFailedCount} — 登录失败累加</li>
 *   <li>{@code resetFailedCount} — 登录成功重置</li>
 *   <li>{@code disableUser} — 管理员禁用</li>
 * </ul>
 *
 * @author 刘家豪
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    // TODO 由刘家豪实现具体 SQL(参考 V1.0__init_auth.sql 的 user 表)
    // 示例:
    // @Select("SELECT * FROM user WHERE username = #{username} AND status = 1 LIMIT 1")
    // User selectByUsername(String username);
}
