package com.peakui.auth.service;

import com.peakui.auth.model.dto.ActivateVipRequest;
import com.peakui.auth.model.dto.LoginRequest;
import com.peakui.auth.model.dto.RegisterRequest;
import com.peakui.auth.model.dto.ReviewCertificationRequest;
import com.peakui.auth.model.dto.SubmitCertificationRequest;
import com.peakui.auth.model.dto.UpdateAvatarRequest;
import com.peakui.auth.model.dto.UpdateCurrentUserRequest;
import com.peakui.auth.model.vo.ActiveDeveloperVO;
import com.peakui.auth.model.vo.AuthTokenVO;
import com.peakui.auth.model.vo.CertificationVO;
import com.peakui.auth.model.vo.FollowStatusVO;
import com.peakui.auth.model.vo.UserBriefVO;
import com.peakui.auth.model.vo.UserFollowVO;
import com.peakui.auth.model.vo.UserProfileVO;
import com.peakui.auth.model.vo.UserRolesVO;
import com.peakui.auth.model.vo.VipStatusVO;
import com.peakui.common.result.PageResponse;

import java.util.List;

public interface AuthService {

    void sendRegisterEmailCode(String email);

    AuthTokenVO register(RegisterRequest request);

    AuthTokenVO login(LoginRequest request);

    com.peakui.auth.model.entity.User authenticate(String account, String password);

    AuthTokenVO loginMiniUser(Long userId);

    AuthTokenVO currentUser();

    AuthTokenVO updateCurrentUser(UpdateCurrentUserRequest request);

    AuthTokenVO updateAvatar(UpdateAvatarRequest request);

    void followUser(Long userId);

    void unfollowUser(Long userId);

    PageResponse<UserFollowVO> listMyFollowing(Long pageNum, Long pageSize);

    PageResponse<UserFollowVO> listMyFollowers(Long pageNum, Long pageSize);

    PageResponse<UserFollowVO> listUserFollowing(Long userId, Long pageNum, Long pageSize);

    PageResponse<UserFollowVO> listUserFollowers(Long userId, Long pageNum, Long pageSize);

    List<ActiveDeveloperVO> listActiveDevelopers();

    void increaseActivityLevel(Long userId);

    FollowStatusVO getFollowStatus(Long userId);

    UserProfileVO getUserProfile(Long userId);

    List<UserBriefVO> getUserBriefs(List<Long> userIds);

    UserProfileVO getUserProfileByUsername(String username);

    UserRolesVO getUserRoles(Long userId);

    VipStatusVO getVipStatus(Long userId);

    void activateVip(Long userId, ActivateVipRequest request);

    CertificationVO submitCertification(SubmitCertificationRequest request);

    PageResponse<CertificationVO> listMyCertifications(Long pageNum, Long pageSize);

    PageResponse<CertificationVO> listCertifications(Integer certType, Integer certStatus, Long pageNum, Long pageSize);

    CertificationVO reviewCertification(Long certificationId, ReviewCertificationRequest request);

    void logout();
}
