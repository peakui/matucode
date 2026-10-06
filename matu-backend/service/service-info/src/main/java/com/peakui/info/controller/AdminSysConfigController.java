package com.peakui.info.controller;

import com.peakui.common.exception.CommonError;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.info.model.dto.CreateSysConfigRequest;
import com.peakui.info.model.dto.UpdateSysConfigRequest;
import com.peakui.info.model.vo.SysConfigVO;
import com.peakui.info.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统配置管理接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/info/sys-configs")
public class AdminSysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "系统配置列表")
    @GetMapping
    public ApiResponse<PageResponse<SysConfigVO>> listSysConfigs(@RequestParam(required = false) String groupName,
                                                                 @RequestParam(required = false) Integer isPublic,
                                                                 @RequestParam(required = false) String keyword,
                                                                 @RequestParam(defaultValue = "1") Long pageNum,
                                                                 @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.success(sysConfigService.listAdminConfigs(groupName, isPublic, keyword, pageNum, pageSize));
    }

    @Operation(summary = "系统配置详情")
    @GetMapping("/{configKey}")
    public ApiResponse<SysConfigVO> getSysConfig(@PathVariable String configKey) {
        return ApiResponse.success(sysConfigService.getAdminConfig(configKey));
    }

    @Operation(summary = "创建系统配置")
    @PostMapping
    public ApiResponse<SysConfigVO> createSysConfig(@Valid @RequestBody CreateSysConfigRequest request) {
        return ApiResponse.success(CommonError.CREATE_SUCCESS.message(), sysConfigService.createSysConfig(request));
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/{configKey}")
    public ApiResponse<SysConfigVO> updateSysConfig(@PathVariable String configKey,
                                                    @Valid @RequestBody UpdateSysConfigRequest request) {
        return ApiResponse.success(CommonError.UPDATE_SUCCESS.message(), sysConfigService.updateSysConfig(configKey, request));
    }

    @Operation(summary = "删除系统配置")
    @DeleteMapping("/{configKey}")
    public ApiResponse<Void> deleteSysConfig(@PathVariable String configKey) {
        sysConfigService.deleteSysConfig(configKey);
        return ApiResponse.success(CommonError.DELETE_SUCCESS.message(), null);
    }
}
