package com.peakui.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.PageResponse;
import com.peakui.interview.exception.InterviewException;
import com.peakui.interview.mapper.InterviewAnswerMapper;
import com.peakui.interview.mapper.InterviewCategoryMapper;
import com.peakui.interview.mapper.InterviewCompanyMapper;
import com.peakui.interview.mapper.InterviewQuestionMapper;
import com.peakui.interview.mapper.MockInterviewMapper;
import com.peakui.interview.mapper.UserQuestionProgressMapper;
import com.peakui.interview.mapper.UserWrongQuestionMapper;
import com.peakui.interview.model.dto.*;
import com.peakui.interview.model.entity.InterviewAnswer;
import com.peakui.interview.model.entity.InterviewCategory;
import com.peakui.interview.model.entity.InterviewCompany;
import com.peakui.interview.model.entity.InterviewQuestion;
import com.peakui.interview.model.entity.MockInterview;
import com.peakui.interview.model.entity.UserQuestionProgress;
import com.peakui.interview.model.entity.UserWrongQuestion;
import com.peakui.interview.model.vo.InterviewAnswerVO;
import com.peakui.interview.model.vo.InterviewCategoryVO;
import com.peakui.interview.model.vo.InterviewCompanyVO;
import com.peakui.interview.model.vo.InterviewQuestionVO;
import com.peakui.interview.model.vo.MockInterviewVO;
import com.peakui.interview.model.vo.UserQuestionProgressVO;
import com.peakui.interview.model.vo.UserWrongQuestionVO;
import com.peakui.interview.service.InterviewService;
import com.peakui.interview.security.InterviewSecurityService;
import com.peakui.interview.feign.AuthFeignClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewQuestionMapper interviewQuestionMapper;
    private final InterviewAnswerMapper interviewAnswerMapper;
    private final InterviewCategoryMapper interviewCategoryMapper;
    private final InterviewCompanyMapper interviewCompanyMapper;
    private final UserQuestionProgressMapper userQuestionProgressMapper;
    private final UserWrongQuestionMapper userWrongQuestionMapper;
    private final MockInterviewMapper mockInterviewMapper;
    private final HttpServletRequest request;
    private final AuthFeignClient authFeignClient;
    private final InterviewSecurityService interviewSecurityService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionVO createQuestion(CreateInterviewQuestionRequest request) {
        interviewSecurityService.requireAdmin();
        InterviewQuestion question = new InterviewQuestion();
        question.setQuestionNo(request.getQuestionNo().trim());
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setAnswer(text(request.getAnswer()));
        question.setCategoryId(request.getCategoryId());
        question.setCompanyId(request.getCompanyId());
        question.setPublisherId(currentUserIdNullable());
        question.setPositionTags(request.getPositionTags() == null ? "[]" : request.getPositionTags().toString());
        question.setDifficulty(request.getDifficulty());
        question.setFrequency(0);
        question.setViewCount(0);
        question.setCollectCount(0);
        question.setIsLocked(request.getIsLocked() == null ? 0 : request.getIsLocked());
        question.setUnlockDays(request.getUnlockDays() == null ? 0 : request.getUnlockDays());
        question.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        interviewQuestionMapper.insert(question);
        log.info("创建面试题成功, questionId={}, questionNo={}, categoryId={}, locked={}",
                question.getId(), question.getQuestionNo(), question.getCategoryId(), question.getIsLocked());
        return toQuestionVO(question, interviewSecurityService.isAdmin());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionVO updateQuestion(Long questionId, UpdateInterviewQuestionRequest request) {
        interviewSecurityService.requireAdmin();
        InterviewQuestion question = getQuestion(questionId);
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setAnswer(text(request.getAnswer()));
        if (request.getCategoryId() != null) {
            question.setCategoryId(request.getCategoryId());
        }
        if (request.getCompanyId() != null) {
            question.setCompanyId(request.getCompanyId());
        }
        if (request.getPositionTags() != null) {
            question.setPositionTags(request.getPositionTags().toString());
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }
        if (request.getFrequency() != null) {
            question.setFrequency(request.getFrequency());
        }
        if (request.getIsLocked() != null) {
            question.setIsLocked(request.getIsLocked());
        }
        if (request.getUnlockDays() != null) {
            question.setUnlockDays(request.getUnlockDays());
        }
        if (request.getStatus() != null) {
            question.setStatus(request.getStatus());
        }
        question.setUpdatedAt(LocalDateTime.now());
        interviewQuestionMapper.updateById(question);
        log.info("更新面试题成功, questionId={}, locked={}", questionId, question.getIsLocked());
        return toQuestionVO(question, interviewSecurityService.isAdmin());
    }

    @Override
    public InterviewQuestionVO getQuestionDetail(Long questionId) {
        InterviewQuestion question = getQuestion(questionId);
        interviewQuestionMapper.update(null, new LambdaUpdateWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getId, questionId)
                .setSql("view_count = IFNULL(view_count, 0) + 1"));
        question.setViewCount((question.getViewCount() == null ? 0 : question.getViewCount()) + 1);
        return toQuestionVO(question, interviewSecurityService.canViewAnswer(question));
    }

    @Override
    public PageResponse<InterviewQuestionVO> listQuestions(Long categoryId, Long companyId, Integer difficulty,
            Integer isLocked, String keyword, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        interviewSecurityService.validatePage(pageNum, pageSize);
        LambdaQueryWrapper<InterviewQuestion> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(InterviewQuestion::getCategoryId, categoryId);
        }
        if (companyId != null) {
            wrapper.eq(InterviewQuestion::getCompanyId, companyId);
        }
        if (difficulty != null) {
            wrapper.eq(InterviewQuestion::getDifficulty, difficulty);
        }
        if (isLocked != null) {
            wrapper.eq(InterviewQuestion::getIsLocked, isLocked);
        }
        wrapper.eq(InterviewQuestion::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InterviewQuestion::getTitle, keyword)
                    .or().like(InterviewQuestion::getQuestionNo, keyword)
                    .or().like(InterviewQuestion::getContent, keyword));
        }
        wrapper.orderByDesc(InterviewQuestion::getCreatedAt);
        Page<InterviewQuestion> page = interviewQuestionMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResponse.of(current, size, page.getTotal(), page.getRecords().stream()
                .map(question -> toQuestionVO(question, interviewSecurityService.canViewAnswer(question))).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewAnswerVO createAnswer(Long questionId, CreateInterviewAnswerRequest request) {
        interviewSecurityService.requireAdmin();
        InterviewQuestion question = getQuestion(questionId);
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestionId(questionId);
        answer.setAnswerType(request.getAnswerType() == null ? 2 : request.getAnswerType());
        answer.setUserId(currentUserIdNullable());
        answer.setContent(request.getContent().trim());
        answer.setContentType(request.getContentType() == null ? 1 : request.getContentType());
        answer.setCodeSnippet(text(request.getCodeSnippet()));
        answer.setLikeCount(0);
        answer.setIsOfficial(request.getIsOfficial() == null ? 0 : request.getIsOfficial());
        answer.setIsLocked(request.getIsLocked() == null ? 0 : request.getIsLocked());
        answer.setUnlockDays(request.getUnlockDays() == null ? 0 : request.getUnlockDays());
        answer.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        answer.setCreatedAt(LocalDateTime.now());
        answer.setUpdatedAt(LocalDateTime.now());
        interviewAnswerMapper.insert(answer);
        log.info("创建面试题答案成功, answerId={}, questionId={}", answer.getId(), questionId);
        return toAnswerVO(answer, interviewSecurityService.canViewAnswer(question));
    }

    @Override
    public List<InterviewAnswerVO> listAnswers(Long questionId) {
        InterviewQuestion question = getQuestion(questionId);
        boolean canViewContent = interviewSecurityService.canViewAnswer(question);
        return interviewAnswerMapper.selectList(new LambdaQueryWrapper<InterviewAnswer>()
                        .eq(InterviewAnswer::getQuestionId, questionId)
                        .eq(InterviewAnswer::getStatus, 1)
                        .orderByDesc(InterviewAnswer::getIsOfficial, InterviewAnswer::getLikeCount, InterviewAnswer::getCreatedAt))
                .stream()
                .map(answer -> toAnswerVO(answer, canViewContent))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewCategoryVO createCategory(CreateInterviewCategoryRequest request) {
        interviewSecurityService.requireAdmin();
        validateCategoryParent(request.getParentId(), null);
        InterviewCategory category = new InterviewCategory();
        category.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryDesc(text(request.getCategoryDesc()));
        category.setIconUrl(text(request.getIconUrl()));
        category.setQuestionCount(0);
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        category.setCreatedAt(LocalDateTime.now());
        interviewCategoryMapper.insert(category);
        log.info("创建面试题分类成功, categoryId={}, name={}", category.getId(), category.getCategoryName());
        return toCategoryVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewCategoryVO updateCategory(Long categoryId, UpdateInterviewCategoryRequest request) {
        interviewSecurityService.requireAdmin();
        InterviewCategory category = getCategory(categoryId);
        validateCategoryParent(request.getParentId(), categoryId);
        category.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryDesc(text(request.getCategoryDesc()));
        category.setIconUrl(text(request.getIconUrl()));
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        interviewCategoryMapper.updateById(category);
        log.info("更新面试题分类成功, categoryId={}, name={}", categoryId, category.getCategoryName());
        return toCategoryVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long categoryId) {
        interviewSecurityService.requireAdmin();
        InterviewCategory category = getCategory(categoryId);
        long childCount = interviewCategoryMapper.selectCount(new LambdaQueryWrapper<InterviewCategory>()
                .eq(InterviewCategory::getParentId, categoryId));
        if (childCount > 0) {
            throw new InterviewException("当前分类下存在子分类，无法删除");
        }
        long questionCount = interviewQuestionMapper.selectCount(new LambdaQueryWrapper<InterviewQuestion>()
                .eq(InterviewQuestion::getCategoryId, categoryId)
                .ne(InterviewQuestion::getStatus, 0));
        if (questionCount > 0) {
            throw new InterviewException("当前分类下存在面试题，无法删除");
        }
        interviewCategoryMapper.deleteById(category.getId());
        log.info("删除面试题分类成功, categoryId={}, name={}", categoryId, category.getCategoryName());
    }

    @Override
    public PageResponse<InterviewCategoryVO> pageCategories(Long parentId, Integer status, String keyword, Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        LambdaQueryWrapper<InterviewCategory> wrapper = new LambdaQueryWrapper<>();
        if (parentId != null) {
            wrapper.eq(InterviewCategory::getParentId, parentId);
        }
        if (status != null) {
            wrapper.eq(InterviewCategory::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InterviewCategory::getCategoryName, keyword)
                    .or().like(InterviewCategory::getCategoryDesc, keyword));
        }
        wrapper.orderByAsc(InterviewCategory::getSortOrder, InterviewCategory::getId);

        Page<InterviewCategory> page = interviewCategoryMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResponse.of(
                current,
                size,
                page.getTotal(),
                page.getRecords().stream().map(this::toCategoryVO).toList()
        );
    }

    @Override
    public List<InterviewCategoryVO> listCategories() {
        List<InterviewCategory> categories = interviewCategoryMapper.selectList(new LambdaQueryWrapper<InterviewCategory>()
                .eq(InterviewCategory::getStatus, 1)
                .orderByAsc(InterviewCategory::getSortOrder, InterviewCategory::getId));
        // 分类表的 question_count 是装饰性的运营数字，卡片要展示真实题目数：按分类聚合已发布题目。
        Map<Long, Long> counts = interviewQuestionMapper.selectList(new LambdaQueryWrapper<InterviewQuestion>()
                        .select(InterviewQuestion::getCategoryId)
                        .eq(InterviewQuestion::getStatus, 1))
                .stream()
                .map(InterviewQuestion::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        categories.forEach(category -> category.setQuestionCount(
                counts.getOrDefault(category.getId(), 0L).intValue()));
        return categories.stream().map(this::toCategoryVO).toList();
    }

    @Override
    public List<InterviewCompanyVO> listCompanies() {
        return interviewCompanyMapper.selectList(new LambdaQueryWrapper<InterviewCompany>()
                        .eq(InterviewCompany::getStatus, 1)
                        .orderByDesc(InterviewCompany::getQuestionCount, InterviewCompany::getId))
                .stream()
                .map(this::toCompanyVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserQuestionProgressVO updateProgress(Long questionId, UpdateQuestionProgressRequest request) {
        getQuestion(questionId);
        Long userId = currentUserId();
        UserQuestionProgress progress = userQuestionProgressMapper.selectOne(new LambdaQueryWrapper<UserQuestionProgress>()
                .eq(UserQuestionProgress::getUserId, userId)
                .eq(UserQuestionProgress::getQuestionId, questionId)
                .eq(UserQuestionProgress::getQuestionType, 1)
                .last("limit 1"));
        if (progress == null) {
            progress = new UserQuestionProgress();
            progress.setUserId(userId);
            progress.setQuestionId(questionId);
            progress.setQuestionType(1);
            progress.setCreatedAt(LocalDateTime.now());
            progress.setPracticeCount(0);
        }
        progress.setStatus(request.getStatus());
        progress.setAnswerContent(text(request.getAnswerContent()));
        progress.setMasteryLevel(request.getMasteryLevel() == null ? progress.getMasteryLevel() : request.getMasteryLevel());
        progress.setPracticeCount((progress.getPracticeCount() == null ? 0 : progress.getPracticeCount())
                + (request.getPracticeCount() == null ? 1 : request.getPracticeCount()));
        progress.setLastPracticeTime(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());
        if (progress.getId() == null) {
            userQuestionProgressMapper.insert(progress);
        } else {
            userQuestionProgressMapper.updateById(progress);
        }
        if (request.getStatus() != null && Objects.equals(request.getStatus(), 3)
                || (request.getAddToWrongBook() != null && Objects.equals(request.getAddToWrongBook(), 1))) {
            saveWrongQuestion(userId, questionId, request.getWrongReason());
        }
        if (request.getStatus() != null && Objects.equals(request.getStatus(), 2)) {
            markWrongQuestionResolved(userId, questionId);
        }
        log.info("更新面试题学习进度, userId={}, questionId={}, status={}, mastery={}",
                userId, questionId, progress.getStatus(), progress.getMasteryLevel());
        return toProgressVO(progress);
    }

    @Override
    public UserQuestionProgressVO getProgress(Long questionId) {
        Long userId = currentUserId();
        UserQuestionProgress progress = userQuestionProgressMapper.selectOne(new LambdaQueryWrapper<UserQuestionProgress>()
                .eq(UserQuestionProgress::getUserId, userId)
                .eq(UserQuestionProgress::getQuestionId, questionId)
                .eq(UserQuestionProgress::getQuestionType, 1)
                .last("limit 1"));
        if (progress == null) {
            throw new InterviewException("学习进度不存在");
        }
        return toProgressVO(progress);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectQuestion(Long questionId) {
        InterviewQuestion question = getQuestion(questionId);
        question.setCollectCount((question.getCollectCount() == null ? 0 : question.getCollectCount()) + 1);
        interviewQuestionMapper.updateById(question);
        log.info("收藏面试题, userId={}, questionId={}", currentUserIdNullable(), questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectQuestion(Long questionId) {
        InterviewQuestion question = getQuestion(questionId);
        int currentCollectCount = question.getCollectCount() == null ? 0 : question.getCollectCount();
        question.setCollectCount(Math.max(0, currentCollectCount - 1));
        interviewQuestionMapper.updateById(question);
        log.info("取消收藏面试题, userId={}, questionId={}", currentUserIdNullable(), questionId);
    }

    @Override
    public PageResponse<InterviewQuestionVO> listCollectedQuestions(Long pageNum, Long pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<InterviewQuestion> page = interviewQuestionMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<InterviewQuestion>()
                .gt(InterviewQuestion::getCollectCount, 0)
                .eq(InterviewQuestion::getStatus, 1)
                .orderByDesc(InterviewQuestion::getCollectCount, InterviewQuestion::getUpdatedAt));
        return PageResponse.of(current, size, page.getTotal(), page.getRecords().stream()
                .map(question -> toQuestionVO(question, interviewSecurityService.canViewAnswer(question))).toList());
    }

    @Override
    public PageResponse<UserWrongQuestionVO> listWrongQuestions(Long pageNum, Long pageSize, Integer isResolved) {
        Long userId = currentUserId();
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<UserWrongQuestion> wrapper = new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getQuestionType, 1)
                .orderByDesc(UserWrongQuestion::getUpdatedAt);
        if (isResolved != null) {
            wrapper.eq(UserWrongQuestion::getIsResolved, isResolved);
        }
        Page<UserWrongQuestion> page = userWrongQuestionMapper.selectPage(new Page<>(current, size), wrapper);
        var ids = page.getRecords().stream().map(UserWrongQuestion::getQuestionId).distinct().toList();
        java.util.Map<Long,String> titles = ids.isEmpty() ? java.util.Map.of() : interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>().select(InterviewQuestion::getId,InterviewQuestion::getTitle)
                        .in(InterviewQuestion::getId,ids)).stream().collect(java.util.stream.Collectors.toMap(
                                InterviewQuestion::getId, q -> q.getTitle() == null ? "未命名题目" : q.getTitle()));
        return PageResponse.of(current, size, page.getTotal(), page.getRecords().stream().map(row -> {
            var vo = toWrongQuestionVO(row);vo.setQuestionTitle(titles.getOrDefault(row.getQuestionId(),"题目已下架"));return vo;
        }).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveWrongQuestion(Long questionId) {
        Long userId = currentUserId();
        markWrongQuestionResolved(userId, questionId);
        log.info("标记错题已解决, userId={}, questionId={}", userId, questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MockInterviewVO createMockInterview(CreateMockInterviewRequest request) {
        Long userId = currentUserId();
        MockInterview interview = new MockInterview();
        interview.setUserId(userId);
        interview.setInterviewTitle(request.getInterviewTitle().trim());
        interview.setPosition(text(request.getPosition()));
        interview.setCompany(text(request.getCompany()));
        interview.setQuestionIds(request.getQuestionIds() == null ? "[]" : request.getQuestionIds().toString());
        interview.setTotalScore(100);
        interview.setUserScore(0);
        interview.setDurationMinutes(request.getDurationMinutes() == null ? 30 : request.getDurationMinutes());
        interview.setActualDuration(0);
        interview.setStatus(0);
        interview.setStartedAt(LocalDateTime.now());
        interview.setCreatedAt(LocalDateTime.now());
        mockInterviewMapper.insert(interview);
        log.info("创建模拟面试成功, mockId={}, userId={}, title={}",
                interview.getId(), userId, interview.getInterviewTitle());
        return toMockInterviewVO(interview);
    }

    @Override
    public PageResponse<MockInterviewVO> listMockInterviews(Long pageNum, Long pageSize) {
        Long userId = currentUserId();
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<MockInterview> page = mockInterviewMapper.selectPage(new Page<>(current, size), new LambdaQueryWrapper<MockInterview>()
                .eq(MockInterview::getUserId, userId)
                .orderByDesc(MockInterview::getCreatedAt));
        return PageResponse.of(current, size, page.getTotal(), page.getRecords().stream().map(this::toMockInterviewVO).toList());
    }

    private InterviewQuestion getQuestion(Long questionId) {
        InterviewQuestion question = interviewQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new InterviewException("面试题不存在");
        }
        return question;
    }

    private Long currentUserId() {
        Long userId = interviewSecurityService.currentUserIdNullable();
        if (userId == null) {
            throw new InterviewException("未登录或登录已失效");
        }
        return userId;
    }

    private Long currentUserIdNullable() {
        return interviewSecurityService.currentUserIdNullable();
    }

    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void saveWrongQuestion(Long userId, Long questionId, String wrongReason) {
        UserWrongQuestion wrongQuestion = userWrongQuestionMapper.selectOne(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getQuestionId, questionId)
                .eq(UserWrongQuestion::getQuestionType, 1)
                .last("limit 1"));
        if (wrongQuestion == null) {
            wrongQuestion = new UserWrongQuestion();
            wrongQuestion.setUserId(userId);
            wrongQuestion.setQuestionId(questionId);
            wrongQuestion.setQuestionType(1);
            wrongQuestion.setWrongCount(0);
            wrongQuestion.setCreatedAt(LocalDateTime.now());
        }
        wrongQuestion.setWrongCount((wrongQuestion.getWrongCount() == null ? 0 : wrongQuestion.getWrongCount()) + 1);
        wrongQuestion.setLastWrongTime(LocalDateTime.now());
        wrongQuestion.setWrongReason(text(wrongReason));
        wrongQuestion.setIsResolved(0);
        wrongQuestion.setResolvedAt(null);
        wrongQuestion.setUpdatedAt(LocalDateTime.now());
        if (wrongQuestion.getId() == null) {
            userWrongQuestionMapper.insert(wrongQuestion);
        } else {
            userWrongQuestionMapper.updateById(wrongQuestion);
        }
    }

    private void markWrongQuestionResolved(Long userId, Long questionId) {
        UserWrongQuestion wrongQuestion = userWrongQuestionMapper.selectOne(new LambdaQueryWrapper<UserWrongQuestion>()
                .eq(UserWrongQuestion::getUserId, userId)
                .eq(UserWrongQuestion::getQuestionId, questionId)
                .eq(UserWrongQuestion::getQuestionType, 1)
                .last("limit 1"));
        if (wrongQuestion == null) {
            return;
        }
        wrongQuestion.setIsResolved(1);
        wrongQuestion.setResolvedAt(LocalDateTime.now());
        wrongQuestion.setUpdatedAt(LocalDateTime.now());
        userWrongQuestionMapper.updateById(wrongQuestion);
    }

    private InterviewQuestionVO toQuestionVO(InterviewQuestion question, boolean canViewContent) {
        InterviewCategory category = question.getCategoryId() == null ? null : interviewCategoryMapper.selectById(question.getCategoryId());
        InterviewCompany company = question.getCompanyId() == null ? null : interviewCompanyMapper.selectById(question.getCompanyId());
        return InterviewQuestionVO.builder()
                .id(question.getId())
                .questionNo(question.getQuestionNo())
                .title(question.getTitle())
                .content(!Integer.valueOf(1).equals(question.getIsLocked()) || canViewContent ? question.getContent() : null)
                .answer(canViewContent ? question.getAnswer() : null)
                .categoryId(question.getCategoryId())
                .categoryName(category == null ? null : category.getCategoryName())
                .companyId(question.getCompanyId())
                .companyName(company == null ? null : company.getCompanyName())
                .publisherId(question.getPublisherId())
                .positionTags(Collections.emptyList())
                .difficulty(question.getDifficulty())
                .frequency(question.getFrequency())
                .viewCount(question.getViewCount())
                .collectCount(question.getCollectCount())
                .isLocked(question.getIsLocked())
                .answerVisible(canViewContent)
                .unlockDays(question.getUnlockDays())
                .status(question.getStatus())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private InterviewAnswerVO toAnswerVO(InterviewAnswer answer, boolean canViewContent) {
        return InterviewAnswerVO.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .answerType(answer.getAnswerType())
                .userId(answer.getUserId())
                .content(canViewContent ? answer.getContent() : null)
                .contentType(answer.getContentType())
                .codeSnippet(canViewContent ? answer.getCodeSnippet() : null)
                .likeCount(answer.getLikeCount())
                .isOfficial(answer.getIsOfficial())
                .isLocked(answer.getIsLocked())
                .unlockDays(answer.getUnlockDays())
                .status(answer.getStatus())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }

    private InterviewCategory getCategory(Long categoryId) {
        InterviewCategory category = interviewCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new InterviewException("分类不存在");
        }
        return category;
    }

    private void validateCategoryParent(Long parentId, Long currentCategoryId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (Objects.equals(parentId, currentCategoryId)) {
            throw new InterviewException("父分类不能是自己");
        }
        InterviewCategory parent = interviewCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new InterviewException("父分类不存在");
        }
    }

    private InterviewCategoryVO toCategoryVO(InterviewCategory category) {
        return InterviewCategoryVO.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .categoryName(category.getCategoryName())
                .categoryDesc(category.getCategoryDesc())
                .iconUrl(category.getIconUrl())
                .questionCount(category.getQuestionCount())
                .sortOrder(category.getSortOrder())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .children(Collections.emptyList())
                .build();
    }

    private InterviewCompanyVO toCompanyVO(InterviewCompany company) {
        return InterviewCompanyVO.builder()
                .id(company.getId())
                .companyName(company.getCompanyName())
                .companyLogo(company.getCompanyLogo())
                .companyType(company.getCompanyType())
                .questionCount(company.getQuestionCount())
                .status(company.getStatus())
                .createdAt(company.getCreatedAt())
                .build();
    }

    private UserQuestionProgressVO toProgressVO(UserQuestionProgress progress) {
        return UserQuestionProgressVO.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .questionId(progress.getQuestionId())
                .questionType(progress.getQuestionType())
                .status(progress.getStatus())
                .answerContent(progress.getAnswerContent())
                .lastPracticeTime(progress.getLastPracticeTime())
                .practiceCount(progress.getPracticeCount())
                .masteryLevel(progress.getMasteryLevel())
                .nextReviewTime(progress.getNextReviewTime())
                .createdAt(progress.getCreatedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    private UserWrongQuestionVO toWrongQuestionVO(UserWrongQuestion wrongQuestion) {
        return UserWrongQuestionVO.builder()
                .id(wrongQuestion.getId())
                .userId(wrongQuestion.getUserId())
                .questionId(wrongQuestion.getQuestionId())
                .questionType(wrongQuestion.getQuestionType())
                .wrongCount(wrongQuestion.getWrongCount())
                .lastWrongTime(wrongQuestion.getLastWrongTime())
                .wrongReason(wrongQuestion.getWrongReason())
                .isResolved(wrongQuestion.getIsResolved())
                .resolvedAt(wrongQuestion.getResolvedAt())
                .createdAt(wrongQuestion.getCreatedAt())
                .updatedAt(wrongQuestion.getUpdatedAt())
                .build();
    }

    private MockInterviewVO toMockInterviewVO(MockInterview interview) {
        return MockInterviewVO.builder()
                .id(interview.getId())
                .userId(interview.getUserId())
                .interviewTitle(interview.getInterviewTitle())
                .position(interview.getPosition())
                .company(interview.getCompany())
                .questionIds(Collections.emptyList())
                .totalScore(interview.getTotalScore())
                .userScore(interview.getUserScore())
                .durationMinutes(interview.getDurationMinutes())
                .actualDuration(interview.getActualDuration())
                .status(interview.getStatus())
                .reportJson(interview.getReportJson())
                .startedAt(interview.getStartedAt())
                .completedAt(interview.getCompletedAt())
                .createdAt(interview.getCreatedAt())
                .build();
    }
}
