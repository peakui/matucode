package com.peakui.auth.mini;

import cn.dev33.satoken.stp.StpUtil;
import com.peakui.auth.mapper.*;
import com.peakui.auth.model.entity.User;
import com.peakui.auth.model.vo.AuthTokenVO;
import com.peakui.auth.service.AuthService;
import com.peakui.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MiniAuthFlowTest {
    private final AuthService auth=mock(AuthService.class);
    private final MiniIdentityService identities=mock(MiniIdentityService.class);
    private final WechatClient wechat=mock(WechatClient.class);
    private final WechatTicketStore tickets=mock(WechatTicketStore.class);
    private final UserProfileMapper profiles=mock(UserProfileMapper.class);
    private final RoleMapper roles=mock(RoleMapper.class);
    private final ExternalIdentityMapper mapper=mock(ExternalIdentityMapper.class);
    private final MiniAuthService service=new MiniAuthService(auth,identities,wechat,tickets,profiles,roles,mapper);
    @BeforeEach void ready(){when(wechat.enabled()).thenReturn(true);when(wechat.appId()).thenReturn("app");}
    @Test void invalidPasswordCannotConsumeTicketOrCreateBinding(){
        when(auth.authenticate("old","wrong")).thenThrow(new BusinessException("账号或密码错误"));
        assertThrows(BusinessException.class,()->service.complete(new MiniModels.CompleteRequest("ticket","BIND","old","wrong")));
        verifyNoInteractions(tickets,identities);
    }
    @Test void ticketFromAnotherAppCannotBind(){
        when(tickets.consume("ticket")).thenReturn(new MiniModels.WechatIdentity("other","openid",null));
        assertThrows(BusinessException.class,()->service.complete(new MiniModels.CompleteRequest("ticket","CREATE",null,null)));
        verifyNoInteractions(identities,auth);
    }
    @Test void loginIsIssuedOnlyAfterCommittedBindingReturns(){
        var identity=new MiniModels.WechatIdentity("app","openid",null);
        when(tickets.consume("ticket")).thenReturn(identity);when(identities.complete(identity,null)).thenReturn(9223372036854775806L);
        User user=new User();user.setId(9223372036854775806L);user.setUsername("wx_user");user.setPasswordHash("!WECHAT_ONLY!");
        when(identities.active(user.getId())).thenReturn(user);when(roles.selectRoleCodesByUserId(user.getId())).thenReturn(List.of("USER"));
        when(auth.loginMiniUser(user.getId())).thenReturn(AuthTokenVO.builder().authorization("Bearer test-token").tokenTimeout(3600).build());
        var result=service.complete(new MiniModels.CompleteRequest("ticket","CREATE",null,null));
        assertEquals("9223372036854775806",result.user().userId());assertFalse(result.user().passwordLoginEnabled());
        var order=inOrder(identities,auth);order.verify(identities).complete(identity,null);order.verify(auth).loginMiniUser(user.getId());
    }
    @Test void bannedAccountCannotReadMe(){
        try(var stp=mockStatic(StpUtil.class)){
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            when(identities.active(7L)).thenThrow(new BusinessException(401,"账号不可用"));
            assertThrows(BusinessException.class,service::me);verifyNoInteractions(profiles,roles);
        }
    }
    @Test void currentAccountBindRequiresReauthenticationBeforeWechatExchange(){
        try(var stp=mockStatic(StpUtil.class)){
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            User user=new User();user.setId(7L);user.setPasswordHash("!WECHAT_ONLY!");when(identities.active(7L)).thenReturn(user);
            assertThrows(BusinessException.class,()->service.bindCurrent(new MiniModels.BindCodeRequest("code","wrong")));
            verify(wechat,never()).exchange(anyString());
        }
    }
}
