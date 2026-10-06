package com.peakui.auth.config;

import cn.dev33.satoken.stp.StpInterface;
import com.peakui.auth.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限扩展：提供当前用户角色集合。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final RoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return roleMapper.selectRoleCodesByUserId(Long.valueOf(String.valueOf(loginId)));
    }
}
