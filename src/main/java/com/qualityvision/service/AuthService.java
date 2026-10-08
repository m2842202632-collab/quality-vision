package com.qualityvision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qualityvision.domain.SysUser;
import com.qualityvision.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 登录：SHA256(salt + password)，UUID token 会话。
 * 与 mes-agent 同一套轻量方案，演示够用。
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;

    @Autowired
    public AuthService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public SysUser login(String username, String password) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        qw.eq(SysUser::getUsername, username);
        SysUser user = userMapper.selectOne(qw);
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!hash(user.getSalt(), password).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return user;
    }

    public static String newSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
