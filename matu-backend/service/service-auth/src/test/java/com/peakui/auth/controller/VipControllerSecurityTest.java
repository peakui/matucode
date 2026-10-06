package com.peakui.auth.controller;

import com.peakui.auth.mapper.CertificationMapper;
import com.peakui.auth.model.dto.ActivateVipRequest;
import com.peakui.auth.model.vo.VipStatusVO;
import com.peakui.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VipControllerSecurityTest {
    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService, mock(CertificationMapper.class));
        ReflectionTestUtils.setField(controller, "vipServiceToken", "test-service-token");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void internalVipReadRequiresServiceToken() throws Exception {
        mockMvc.perform(get("/auth/internal/users/7/vip"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authService);
    }

    @Test
    void internalVipReadAcceptsConfiguredServiceToken() throws Exception {
        when(authService.getVipStatus(7L)).thenReturn(VipStatusVO.builder().userId(7L).valid(false).build());
        mockMvc.perform(get("/auth/internal/users/7/vip")
                        .header("X-Vip-Service-Token", "test-service-token"))
                .andExpect(status().isOk());
        verify(authService).getVipStatus(7L);
    }

    @Test
    void internalVipActivationRequiresServiceToken() throws Exception {
        mockMvc.perform(post("/auth/internal/users/7/vip/activate")
                        .contentType(APPLICATION_JSON)
                        .content("{\"activationKey\":\"order-1\",\"days\":31,\"level\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authService);
    }

    @Test
    void internalVipActivationPassesValidatedRequest() throws Exception {
        mockMvc.perform(post("/auth/internal/users/7/vip/activate")
                        .header("X-Vip-Service-Token", "test-service-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"activationKey\":\"order-1\",\"days\":31,\"level\":1}"))
                .andExpect(status().isOk());
        verify(authService).activateVip(eq(7L), any(ActivateVipRequest.class));
    }
}
