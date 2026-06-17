package com.mavis.evalplatform.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mavis.evalplatform.auth.entity.User;
import com.mavis.evalplatform.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security UserDetailsService 实现
 * <p>
 * 由【刘家豪 FR-01】实现。
 * <p>
 * 关键点:
 * <ul>
 *   <li>按 username 查 DB,只查 status=1 的</li>
 *   <li>角色加 ROLE_ 前缀,如 ROLE_ADMIN</li>
 *   <li>找不到用户抛 {@code UsernameNotFoundException}</li>
 * </ul>
 *
 * @author 刘家豪
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO 由刘家豪实现:查 user 表,只取 status=1 的,角色加 ROLE_ 前缀
        throw new UnsupportedOperationException("TODO 由刘家豪 FR-01 实现 loadUserByUsername");
    }
}
