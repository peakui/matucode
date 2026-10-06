package com.peakui.info.controller;

import com.peakui.common.result.ApiResponse;
import com.peakui.info.model.vo.SysConfigVO;
import com.peakui.info.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "公开配置接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/configs")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "公开配置列表")
    @GetMapping("/public")
    public ApiResponse<List<SysConfigVO>> listPublicConfigs(@RequestParam(required = false) String groupName) {
        return ApiResponse.success(sysConfigService.listPublicConfigs(groupName));
    }

    @Operation(summary = "公开配置详情")
    @GetMapping("/public/{configKey}")
    public ApiResponse<SysConfigVO> getPublicConfig(@PathVariable String configKey) {
        return ApiResponse.success(sysConfigService.getPublicConfig(configKey));
    }
}
