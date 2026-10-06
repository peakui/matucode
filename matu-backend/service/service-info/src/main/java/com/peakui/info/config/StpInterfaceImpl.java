package com.peakui.info.config;

import cn.dev33.satoken.stp.StpInterface;
import com.peakui.common.result.ApiResponse;
import com.peakui.info.feign.AuthRoleFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final AuthRoleFeignClient authRoleFeignClient;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return Collections.emptyList();
        }
        try {
            ApiResponse<AuthRoleFeignClient.UserRolesResponse> response = authRoleFeignClient.getUserRoles(Long.valueOf(String.valueOf(loginId)));
            AuthRoleFeignClient.UserRolesResponse data = response == null ? null : response.getData();
            return data == null || data.getRoles() == null ? Collections.emptyList() : data.getRoles();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
