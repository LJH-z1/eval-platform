package com.mavis.evalplatform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mavis.evalplatform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * User Mapper
 *
 * @author 刘家豪
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username} AND status = 1 LIMIT 1")
    User selectByUsername(String username);

    @Update("UPDATE user SET failed_count = failed_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementFailedCount(Long id);

    @Update("UPDATE user SET failed_count = 0, locked_until = NULL, updated_at = NOW() WHERE id = #{id}")
    int resetFailedCount(Long id);
}
