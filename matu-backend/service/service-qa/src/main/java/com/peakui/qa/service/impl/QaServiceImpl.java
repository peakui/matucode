package com.peakui.qa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.notify.CommentReplyNotifyRequest;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.qa.exception.QaException;
import com.peakui.qa.feign.MessageFeignClient;
import com.peakui.qa.feign.UserFeignClient;
import com.peakui.qa.feign.vo.UserProfileVO;
import com.peakui.qa.mapper.QaAnswerMapper;
import com.peakui.qa.mapper.QaCategoryMapper;
import com.peakui.qa.mapper.QaCommentLikeMapper;
import com.peakui.qa.mapper.QaCommentMapper;
import com.peakui.qa.mapper.QaFollowMapper;
import com.peakui.qa.mapper.QaQuestionMapper;
import com.peakui.qa.mapper.QaVoteMapper;
import com.peakui.qa.mapper.QaWordCloudMapper;
import com.peakui.qa.model.dto.CreateAnswerRequest;
import com.peakui.qa.model.dto.CreateQaCommentRequest;
import com.peakui.qa.model.dto.CreateQuestionRequest;
import com.peakui.qa.model.dto.UpdateAnswerRequest;
import com.peakui.qa.model.dto.UpdateQuestionRequest;
import com.peakui.qa.model.entity.QaAnswer;
import com.peakui.qa.model.entity.QaCategory;
import com.peakui.qa.model.entity.QaComment;
import com.peakui.qa.model.entity.QaCommentLike;
import com.peakui.qa.model.entity.QaFollow;
import com.peakui.qa.model.entity.QaQuestion;
import com.peakui.qa.model.entity.QaVote;
import com.peakui.qa.model.entity.QaWordCloud;
import com.peakui.qa.model.vo.QaAnswerVO;
import com.peakui.qa.model.vo.QaCategoryVO;
import com.peakui.qa.model.vo.QaCommentVO;
import com.peakui.qa.model.vo.QaQuestionDetailVO;
import com.peakui.qa.model.vo.QaQuestionListItemVO;
import com.peakui.qa.model.vo.QaWordCloudVO;
import com.peakui.qa.service.QaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 问答业务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    private static final int QUESTION_STATUS_UNSOLVED = 0;
    private static final int QUESTION_STATUS_SOLVED = 1;
    private static final int QUESTION_STATUS_CLOSED = 2;
    private static final int QUESTION_STATUS_DELETED = 3;
    private static final int ANSWER_STATUS_VISIBLE = 1;
    private static final int ANSWER_STATUS_DELETED = 3;
    private static final int COMMENT_STATUS_NORMAL = 1;
    private static final int COMMENT_STATUS_DELETED = 3;
    private static final int TARGET_TYPE_QUESTION = 1;
    private static final int TARGET_TYPE_ANSWER = 2;
    private static final int VOTE_TYPE_LIKE = 1;
    private static final int VOTE_TYPE_DISLIKE = 2;

    private final QaQuestionMapper qaQuestionMapper;
    private final QaAnswerMapper qaAnswerMapper;
    private final QaCommentMapper qaCommentMapper;
    private final QaCommentLikeMapper qaCommentLikeMapper;
    private final QaCategoryMapper qaCategoryMapper;
    private final QaVoteMapper qaVoteMapper;
    private final QaFollowMapper qaFollowMapper;
    private final QaWordCloudMapper qaWordCloudMapper;
    private final UserFeignClient userFeignClient;
    private final MessageFeignClient messageFeignClient;
    private final HttpServletRequest servletRequest;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaQuestionDetailVO createQuestion(CreateQuestionRequest request) {
        Long currentUserId = getCurrentUserId();
        QaCategory category = request.getCategoryId() == null ? null : getCategory(request.getCategoryId());
        LocalDateTime now = LocalDateTime.now();

        QaQuestion question = new QaQuestion();
        question.setUserId(currentUserId);
        question.setCategoryId(request.getCategoryId());
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setBountyPoints(request.getBountyPoints() == null ? 0 : Math.max(request.getBountyPoints(), 0));
        question.setViewCount(0);
        question.setAnswerCount(0);
        question.setFollowCount(0);
        question.setStatus(QUESTION_STATUS_UNSOLVED);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);
        qaQuestionMapper.insert(question);

        if (category != null) {
            qaCategoryMapper.update(null, new LambdaUpdateWrapper<QaCategory>()
                    .eq(QaCategory::getId, category.getId())
                    .setSql("question_count = IFNULL(question_count, 0) + 1"));
        }
        increaseActivityLevel(currentUserId);
        log.info("发布问答成功, questionId={}, userId={}, categoryId={}, bounty={}",
                question.getId(), currentUserId, question.getCategoryId(), question.getBountyPoints());
        return getQuestionDetail(question.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaQuestionDetailVO updateQuestion(Long questionId, UpdateQuestionRequest request) {
        Long currentUserId = getCurrentUserId();
        QaQuestion question = getOwnedQuestion(questionId, currentUserId);
        if (request.getCategoryId() != null) {
            getCategory(request.getCategoryId());
            question.setCategoryId(request.getCategoryId());
        }
        if (StringUtils.hasText(request.getTitle())) {
            question.setTitle(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getContent())) {
            question.setContent(request.getContent().trim());
        }
        if (request.getBountyPoints() != null) {
            question.setBountyPoints(Math.max(request.getBountyPoints(), 0));
        }
        if (request.getStatus() != null) {
            validateQuestionStatus(request.getStatus());
            question.setStatus(request.getStatus());
            if (Objects.equals(request.getStatus(), QUESTION_STATUS_SOLVED) && question.getSolvedAt() == null) {
                question.setSolvedAt(LocalDateTime.now());
            }
        }
        question.setUpdatedAt(LocalDateTime.now());
        qaQuestionMapper.updateById(question);
        log.info("更新问答成功, questionId={}, userId={}, status={}",
                questionId, currentUserId, question.getStatus());
        return getQuestionDetail(questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(Long questionId) {
        Long currentUserId = getCurrentUserId();
        QaQuestion question = getOwnedQuestion(questionId, currentUserId);
        if (Objects.equals(question.getStatus(), QUESTION_STATUS_DELETED)) {
            return;
        }
        question.setStatus(QUESTION_STATUS_DELETED);
        question.setUpdatedAt(LocalDateTime.now());
        qaQuestionMapper.updateById(question);
        qaAnswerMapper.update(null, new LambdaUpdateWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, questionId)
                .set(QaAnswer::getStatus, ANSWER_STATUS_DELETED));
        qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                .eq(QaComment::getTargetType, TARGET_TYPE_QUESTION)
                .eq(QaComment::getTargetId, questionId)
                .set(QaComment::getStatus, COMMENT_STATUS_DELETED));
        qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                .in(QaComment::getTargetId, qaAnswerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                        .eq(QaAnswer::getQuestionId, questionId)
                        .select(QaAnswer::getId)).stream().map(QaAnswer::getId).toList())
                .eq(QaComment::getTargetType, TARGET_TYPE_ANSWER)
                .set(QaComment::getStatus, COMMENT_STATUS_DELETED));
        qaFollowMapper.delete(new LambdaQueryWrapper<QaFollow>()
                .eq(QaFollow::getQuestionId, questionId));
        qaVoteMapper.delete(new LambdaQueryWrapper<QaVote>()
                .and(wrapper -> wrapper
                        .eq(QaVote::getTargetType, TARGET_TYPE_QUESTION)
                        .eq(QaVote::getTargetId, questionId)));
        qaWordCloudMapper.delete(new LambdaQueryWrapper<QaWordCloud>()
                .eq(QaWordCloud::getQuestionId, questionId));
        log.info("删除问答及级联数据, questionId={}, userId={}", questionId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaQuestionDetailVO getQuestionDetail(Long questionId) {
        Long currentUserId = getCurrentUserIdNullable();
        QaQuestion question = getVisibleQuestion(questionId);
        qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, questionId)
                .setSql("view_count = IFNULL(view_count, 0) + 1"));
        question.setViewCount(safeInt(question.getViewCount()) + 1);
        return toQuestionDetailVO(question, currentUserId);
    }

    @Override
    public PageResponse<QaQuestionListItemVO> listQuestions(Long categoryId, Integer status, String keyword,
                                                            Long pageNum, Long pageSize, String sortBy) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<QaQuestion> wrapper = new LambdaQueryWrapper<QaQuestion>()
                .ne(QaQuestion::getStatus, QUESTION_STATUS_DELETED);
        if (categoryId != null) {
            wrapper.eq(QaQuestion::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(QaQuestion::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(QaQuestion::getTitle, keyword).or().like(QaQuestion::getContent, keyword));
        }
        applyQuestionSort(wrapper, sortBy);

        Page<QaQuestion> page = qaQuestionMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        return PageResponse.of(currentPage, currentSize, page.getTotal(), buildQuestionListItems(page.getRecords()));
    }

    @Override
    public PageResponse<QaQuestionListItemVO> listLikedQuestions(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<QaVote> page = qaVoteMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<QaVote>()
                        .eq(QaVote::getUserId, currentUserId)
                        .eq(QaVote::getTargetType, TARGET_TYPE_QUESTION)
                        .eq(QaVote::getVoteType, VOTE_TYPE_LIKE)
                        .orderByDesc(QaVote::getCreatedAt));
        List<Long> questionIds = page.getRecords().stream().map(QaVote::getTargetId).distinct().toList();
        if (questionIds.isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        Map<Long, QaQuestion> questionMap = qaQuestionMapper.selectList(new LambdaQueryWrapper<QaQuestion>()
                        .in(QaQuestion::getId, questionIds)
                        .ne(QaQuestion::getStatus, QUESTION_STATUS_DELETED))
                .stream().collect(Collectors.toMap(QaQuestion::getId, question -> question, (a, b) -> a));
        List<QaQuestion> ordered = questionIds.stream().map(questionMap::get).filter(Objects::nonNull).toList();
        return PageResponse.of(currentPage, currentSize, page.getTotal(), buildQuestionListItems(ordered));
    }

    @Override
    public PageResponse<QaQuestionListItemVO> listMyQuestions(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        return listQuestionPage(new LambdaQueryWrapper<QaQuestion>()
                .eq(QaQuestion::getUserId, currentUserId)
                .ne(QaQuestion::getStatus, QUESTION_STATUS_DELETED)
                .orderByDesc(QaQuestion::getCreatedAt), pageNum, pageSize);
    }

    @Override
    public PageResponse<QaQuestionListItemVO> listMyFollowedQuestions(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        List<QaFollow> follows = qaFollowMapper.selectList(new LambdaQueryWrapper<QaFollow>()
                .eq(QaFollow::getUserId, currentUserId)
                .orderByDesc(QaFollow::getCreatedAt));
        if (follows.isEmpty()) {
            long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
            long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
            return PageResponse.of(currentPage, currentSize, 0, Collections.emptyList());
        }
        List<Long> questionIds = follows.stream().map(QaFollow::getQuestionId).distinct().toList();
        return listQuestionPage(new LambdaQueryWrapper<QaQuestion>()
                .in(QaQuestion::getId, questionIds)
                .ne(QaQuestion::getStatus, QUESTION_STATUS_DELETED)
                .orderByDesc(QaQuestion::getCreatedAt), pageNum, pageSize);
    }

    @Override
    public PageResponse<QaAnswerVO> listQuestionAnswers(Long questionId, Long pageNum, Long pageSize) {
        getVisibleQuestion(questionId);
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<QaAnswer> page = qaAnswerMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<QaAnswer>()
                        .eq(QaAnswer::getQuestionId, questionId)
                        .eq(QaAnswer::getStatus, ANSWER_STATUS_VISIBLE)
                        .orderByDesc(QaAnswer::getIsAccepted, QaAnswer::getLikeCount, QaAnswer::getCreatedAt));
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(page.getRecords().stream().map(QaAnswer::getUserId).collect(Collectors.toSet()));
        List<QaAnswerVO> items = page.getRecords().stream().map(answer -> toAnswerVO(answer, userProfiles)).toList();
        return PageResponse.of(currentPage, currentSize, page.getTotal(), items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaAnswerVO createAnswer(Long questionId, CreateAnswerRequest request) {
        QaQuestion question = getVisibleQuestion(questionId);
        Long currentUserId = getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        QaAnswer answer = new QaAnswer();
        answer.setQuestionId(questionId);
        answer.setUserId(currentUserId);
        answer.setContent(request.getContent().trim());
        answer.setLikeCount(0);
        answer.setDislikeCount(0);
        answer.setIsAccepted(0);
        answer.setStatus(ANSWER_STATUS_VISIBLE);
        answer.setCreatedAt(now);
        answer.setUpdatedAt(now);
        qaAnswerMapper.insert(answer);

        qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, questionId)
                .setSql("answer_count = IFNULL(answer_count, 0) + 1"));
        increaseActivityLevel(currentUserId);
        log.info("新增回答成功, answerId={}, questionId={}, userId={}",
                answer.getId(), questionId, currentUserId);
        return toAnswerVO(qaAnswerMapper.selectById(answer.getId()), loadUserProfiles(Set.of(currentUserId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaAnswerVO updateAnswer(Long answerId, UpdateAnswerRequest request) {
        Long currentUserId = getCurrentUserId();
        QaAnswer answer = getOwnedAnswer(answerId, currentUserId);
        answer.setContent(request.getContent().trim());
        answer.setUpdatedAt(LocalDateTime.now());
        qaAnswerMapper.updateById(answer);
        log.info("更新回答成功, answerId={}, userId={}", answerId, currentUserId);
        return toAnswerVO(answer, loadUserProfiles(Set.of(answer.getUserId())));
    }

    @Override
    public PageResponse<QaAnswerVO> listMyAnswers(Long pageNum, Long pageSize) {
        Long currentUserId = getCurrentUserId();
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<QaAnswer> page = qaAnswerMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<QaAnswer>()
                        .eq(QaAnswer::getUserId, currentUserId)
                        .ne(QaAnswer::getStatus, ANSWER_STATUS_DELETED)
                        .orderByDesc(QaAnswer::getCreatedAt));
        if (page.getRecords().isEmpty()) {
            return PageResponse.of(currentPage, currentSize, page.getTotal(), Collections.emptyList());
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(Set.of(currentUserId));
        List<QaAnswerVO> items = page.getRecords().stream()
                .map(answer -> toAnswerVO(answer, userProfiles))
                .toList();
        return PageResponse.of(currentPage, currentSize, page.getTotal(), items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnswer(Long answerId) {
        Long currentUserId = getCurrentUserId();
        QaAnswer answer = getOwnedAnswer(answerId, currentUserId);
        if (Objects.equals(answer.getStatus(), ANSWER_STATUS_DELETED)) {
            return;
        }
        answer.setStatus(ANSWER_STATUS_DELETED);
        answer.setUpdatedAt(LocalDateTime.now());
        qaAnswerMapper.updateById(answer);
        qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, answer.getQuestionId())
                .setSql("answer_count = GREATEST(IFNULL(answer_count, 0) - 1, 0)"));
        qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                .eq(QaComment::getTargetType, TARGET_TYPE_ANSWER)
                .eq(QaComment::getTargetId, answerId)
                .set(QaComment::getStatus, COMMENT_STATUS_DELETED));
        log.info("删除回答成功, answerId={}, questionId={}, userId={}",
                answerId, answer.getQuestionId(), currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptAnswer(Long questionId, Long answerId) {
        Long currentUserId = getCurrentUserId();
        QaQuestion question = getVisibleQuestion(questionId);
        if (!Objects.equals(question.getUserId(), currentUserId) && !isAdmin()) {
            throw new QaException("仅提问者可采纳回答");
        }
        QaAnswer answer = qaAnswerMapper.selectById(answerId);
        if (answer == null || !Objects.equals(answer.getQuestionId(), questionId) || !Objects.equals(answer.getStatus(), ANSWER_STATUS_VISIBLE)) {
            throw new QaException("回答不存在");
        }
        if (Objects.equals(answer.getIsAccepted(), 1)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        answer.setIsAccepted(1);
        answer.setAcceptedAt(now);
        qaAnswerMapper.updateById(answer);

        question.setStatus(QUESTION_STATUS_SOLVED);
        question.setBestAnswerId(answerId);
        question.setSolvedAt(now);
        question.setUpdatedAt(now);
        qaQuestionMapper.updateById(question);
        log.info("采纳回答成功, questionId={}, answerId={}, operatorId={}",
                questionId, answerId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followQuestion(Long questionId) {
        QaQuestion question = getVisibleQuestion(questionId);
        Long currentUserId = getCurrentUserId();
        long count = qaFollowMapper.selectCount(new LambdaQueryWrapper<QaFollow>()
                .eq(QaFollow::getQuestionId, questionId)
                .eq(QaFollow::getUserId, currentUserId));
        if (count > 0) {
            return;
        }
        QaFollow follow = new QaFollow();
        follow.setQuestionId(questionId);
        follow.setUserId(currentUserId);
        follow.setCreatedAt(LocalDateTime.now());
        qaFollowMapper.insert(follow);
        qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, question.getId())
                .setSql("follow_count = IFNULL(follow_count, 0) + 1"));
        log.info("关注问题, questionId={}, userId={}", questionId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowQuestion(Long questionId) {
        Long currentUserId = getCurrentUserId();
        QaFollow follow = qaFollowMapper.selectOne(new LambdaQueryWrapper<QaFollow>()
                .eq(QaFollow::getQuestionId, questionId)
                .eq(QaFollow::getUserId, currentUserId)
                .last("limit 1"));
        if (follow == null) {
            return;
        }
        qaFollowMapper.deleteById(follow.getId());
        qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, questionId)
                .setSql("follow_count = GREATEST(IFNULL(follow_count, 0) - 1, 0)"));
        log.info("取消关注问题, questionId={}, userId={}", questionId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shareQuestion(Long questionId) {
        QaQuestion question = getVisibleQuestion(questionId);
        qaQuestionMapper.update(null, new LambdaUpdateWrapper<QaQuestion>()
                .eq(QaQuestion::getId, question.getId())
                .setSql("share_count = IFNULL(share_count, 0) + 1"));
        log.info("分享问题, questionId={}", questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voteQuestion(Long questionId, Integer voteType) {
        getVisibleQuestion(questionId);
        saveVote(TARGET_TYPE_QUESTION, questionId, voteType, null);
        log.info("问题投票, questionId={}, userId={}, voteType={}",
                questionId, getCurrentUserId(), voteType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unvoteQuestion(Long questionId) {
        Long currentUserId = getCurrentUserId();
        qaVoteMapper.delete(new LambdaQueryWrapper<QaVote>()
                .eq(QaVote::getTargetType, TARGET_TYPE_QUESTION)
                .eq(QaVote::getTargetId, questionId)
                .eq(QaVote::getUserId, currentUserId));
        log.info("取消问题投票, questionId={}, userId={}", questionId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voteAnswer(Long answerId, Integer voteType) {
        QaAnswer answer = qaAnswerMapper.selectById(answerId);
        if (answer == null || !Objects.equals(answer.getStatus(), ANSWER_STATUS_VISIBLE)) {
            throw new QaException("回答不存在");
        }
        saveVote(TARGET_TYPE_ANSWER, answerId, voteType, answer);
        log.info("回答投票, answerId={}, userId={}, voteType={}",
                answerId, getCurrentUserId(), voteType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QaCommentVO createComment(CreateQaCommentRequest request) {
        validateCommentTarget(request.getTargetType(), request.getTargetId());
        Long currentUserId = getCurrentUserId();
        QaComment parent = null;
        if (request.getParentId() != null && request.getParentId() > 0) {
            parent = qaCommentMapper.selectById(request.getParentId());
            if (parent == null
                    || !Objects.equals(parent.getTargetType(), request.getTargetType())
                    || !Objects.equals(parent.getTargetId(), request.getTargetId())) {
                throw new QaException("父评论不存在");
            }
        }
        QaComment comment = new QaComment();
        comment.setTargetType(request.getTargetType());
        comment.setTargetId(request.getTargetId());
        comment.setUserId(currentUserId);
        comment.setParentId(parent == null ? 0L : parent.getId());
        Long replyToUserId = request.getReplyToUserId() != null
                ? request.getReplyToUserId()
                : (parent == null ? null : parent.getUserId());
        comment.setReplyToUserId(replyToUserId);
        comment.setReplyCount(0);
        comment.setContent(request.getContent().trim());
        comment.setLikeCount(0);
        comment.setStatus(COMMENT_STATUS_NORMAL);
        comment.setCreatedAt(LocalDateTime.now());
        qaCommentMapper.insert(comment);

        if (parent != null) {
            qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                    .eq(QaComment::getId, parent.getId())
                    .setSql("reply_count = IFNULL(reply_count, 0) + 1"));
        }

        notifyCommentReply(request.getTargetType(), request.getTargetId(), comment, parent, currentUserId);
        log.info("新增问答评论成功, commentId={}, targetType={}, targetId={}, userId={}",
                comment.getId(), comment.getTargetType(), comment.getTargetId(), currentUserId);
        return toCommentVO(comment, loadUserProfiles(Set.of(currentUserId)), Collections.emptySet());
    }

    private void notifyCommentReply(Integer targetType, Long targetId, QaComment comment,
                                    QaComment parent, Long currentUserId) {
        try {
            Long contentOwnerId;
            String sourceTitle;
            Long sourceId;
            String sourceType;
            if (Objects.equals(targetType, TARGET_TYPE_QUESTION)) {
                QaQuestion question = qaQuestionMapper.selectById(targetId);
                contentOwnerId = question == null ? null : question.getUserId();
                sourceTitle = question == null ? null : question.getTitle();
                sourceId = targetId;
                sourceType = "question";
            } else {
                QaAnswer answer = qaAnswerMapper.selectById(targetId);
                contentOwnerId = answer == null ? null : answer.getUserId();
                QaQuestion question = answer == null ? null : qaQuestionMapper.selectById(answer.getQuestionId());
                sourceTitle = question == null ? null : question.getTitle();
                sourceId = answer == null ? targetId : answer.getQuestionId();
                sourceType = "answer";
            }
            Set<Long> recipients = new LinkedHashSet<>();
            if (contentOwnerId != null && !Objects.equals(contentOwnerId, currentUserId)) {
                recipients.add(contentOwnerId);
            }
            if (parent != null && parent.getUserId() != null && !Objects.equals(parent.getUserId(), currentUserId)) {
                recipients.add(parent.getUserId());
            }
            if (recipients.isEmpty()) {
                return;
            }
            UserProfileVO profile = fetchUserProfile(currentUserId);
            String fromNickname = resolveUserName(profile, currentUserId);
            String fromAvatar = resolveUserAvatar(profile);
            String preview = comment.getContent() == null ? null
                    : comment.getContent().substring(0, Math.min(200, comment.getContent().length()));
            List<CommentReplyNotifyRequest> requests = recipients.stream()
                    .map(toUserId -> {
                        CommentReplyNotifyRequest notify = new CommentReplyNotifyRequest();
                        notify.setSourceType(sourceType);
                        notify.setSourceId(sourceId);
                        notify.setSourceTitle(sourceTitle);
                        notify.setCommentId(comment.getId());
                        notify.setParentCommentId(comment.getParentId());
                        notify.setFromUserId(currentUserId);
                        notify.setFromNickname(fromNickname);
                        notify.setFromAvatar(fromAvatar);
                        notify.setToUserId(toUserId);
                        notify.setContentPreview(preview);
                        return notify;
                    })
                    .toList();
            sendCommentReplyNotifications(requests);
        } catch (Exception e) {
            log.warn("构建评论回复通知失败, targetType={}, targetId={}, commentId={}",
                    targetType, targetId, comment.getId(), e);
        }
    }

    private void sendCommentReplyNotifications(List<CommentReplyNotifyRequest> requests) {
        Runnable task = () -> requests.forEach(notify -> {
            try {
                messageFeignClient.notifyCommentReply(notify);
            } catch (Exception e) {
                log.warn("发送评论回复通知失败, toUserId={}", notify.getToUserId(), e);
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long currentUserId = getCurrentUserId();
        QaComment comment = qaCommentMapper.selectById(commentId);
        if (comment == null || Objects.equals(comment.getStatus(), COMMENT_STATUS_DELETED)) {
            return;
        }
        if (!Objects.equals(comment.getUserId(), currentUserId) && !isAdmin()) {
            throw new QaException("仅评论作者可删除评论");
        }
        qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                .eq(QaComment::getId, commentId)
                .or()
                .eq(QaComment::getParentId, commentId)
                .set(QaComment::getStatus, COMMENT_STATUS_DELETED));
        log.info("删除问答评论成功, commentId={}, operatorId={}", commentId, currentUserId);
    }

    @Override
    public List<QaCommentVO> listComments(Integer targetType, Long targetId) {
        validateCommentTarget(targetType, targetId);
        List<QaComment> comments = qaCommentMapper.selectList(new LambdaQueryWrapper<QaComment>()
                .eq(QaComment::getTargetType, targetType)
                .eq(QaComment::getTargetId, targetId)
                .eq(QaComment::getStatus, COMMENT_STATUS_NORMAL)
                .orderByAsc(QaComment::getCreatedAt));
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(comments.stream().map(QaComment::getUserId).collect(Collectors.toSet()));
        Set<Long> likedCommentIds = loadLikedCommentIds(comments, getCurrentUserIdNullable());
        Map<Long, List<QaCommentVO>> childMap = new LinkedHashMap<>();
        List<QaCommentVO> roots = new ArrayList<>();
        for (QaComment comment : comments) {
            QaCommentVO vo = toCommentVO(comment, userProfiles, likedCommentIds);
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                roots.add(vo);
            } else {
                childMap.computeIfAbsent(comment.getParentId(), key -> new ArrayList<>()).add(vo);
            }
        }
        roots.forEach(root -> appendChildren(root, childMap));
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        Long currentUserId = getCurrentUserId();
        requireVisibleComment(commentId);
        long existing = qaCommentLikeMapper.selectCount(new LambdaQueryWrapper<QaCommentLike>()
                .eq(QaCommentLike::getCommentId, commentId)
                .eq(QaCommentLike::getUserId, currentUserId));
        if (existing > 0) {
            return;
        }
        QaCommentLike like = new QaCommentLike();
        like.setCommentId(commentId);
        like.setUserId(currentUserId);
        like.setCreatedAt(LocalDateTime.now());
        qaCommentLikeMapper.insert(like);
        qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                .eq(QaComment::getId, commentId)
                .setSql("like_count = IFNULL(like_count, 0) + 1"));
        log.info("点赞问答评论成功, commentId={}, userId={}", commentId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId) {
        Long currentUserId = getCurrentUserId();
        requireVisibleComment(commentId);
        QaCommentLike like = qaCommentLikeMapper.selectOne(new LambdaQueryWrapper<QaCommentLike>()
                .eq(QaCommentLike::getCommentId, commentId)
                .eq(QaCommentLike::getUserId, currentUserId)
                .last("limit 1"));
        if (like == null) {
            return;
        }
        qaCommentLikeMapper.deleteById(like.getId());
        qaCommentMapper.update(null, new LambdaUpdateWrapper<QaComment>()
                .eq(QaComment::getId, commentId)
                .setSql("like_count = GREATEST(IFNULL(like_count, 0) - 1, 0)"));
        log.info("取消点赞问答评论成功, commentId={}, userId={}", commentId, currentUserId);
    }

    private QaComment requireVisibleComment(Long commentId) {
        QaComment comment = qaCommentMapper.selectById(commentId);
        if (comment == null || !Objects.equals(comment.getStatus(), COMMENT_STATUS_NORMAL)) {
            throw new QaException("评论不存在");
        }
        return comment;
    }

    private Set<Long> loadLikedCommentIds(List<QaComment> comments, Long currentUserId) {
        if (currentUserId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> commentIds = comments.stream().map(QaComment::getId).filter(Objects::nonNull).toList();
        if (commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        return qaCommentLikeMapper.selectList(new LambdaQueryWrapper<QaCommentLike>()
                        .eq(QaCommentLike::getUserId, currentUserId)
                        .in(QaCommentLike::getCommentId, commentIds))
                .stream()
                .map(QaCommentLike::getCommentId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<QaCategoryVO> listCategories() {
        return qaCategoryMapper.selectList(new LambdaQueryWrapper<QaCategory>()
                        .eq(QaCategory::getStatus, 1)
                        .orderByAsc(QaCategory::getSortOrder, QaCategory::getId))
                .stream()
                .map(category -> QaCategoryVO.builder()
                        .id(category.getId())
                        .parentId(category.getParentId())
                        .categoryName(category.getCategoryName())
                        .categoryDesc(category.getCategoryDesc())
                        .iconUrl(category.getIconUrl())
                        .sortOrder(category.getSortOrder())
                        .questionCount(category.getQuestionCount())
                        .children(new ArrayList<>())
                        .build())
                .toList();
    }

    @Override
    public List<QaCategoryVO> listCategoryTree() {
        List<QaCategoryVO> categories = listCategories();
        if (categories.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, QaCategoryVO> categoryMap = categories.stream()
                .collect(Collectors.toMap(QaCategoryVO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        List<QaCategoryVO> roots = new ArrayList<>();
        for (QaCategoryVO category : categories) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                roots.add(category);
                continue;
            }
            QaCategoryVO parent = categoryMap.get(category.getParentId());
            if (parent == null) {
                roots.add(category);
            } else {
                parent.getChildren().add(category);
            }
        }
        return roots;
    }

    @Override
    public QaWordCloudVO getWordCloud(Long questionId) {
        getVisibleQuestion(questionId);
        QaWordCloud wordCloud = qaWordCloudMapper.selectOne(new LambdaQueryWrapper<QaWordCloud>()
                .eq(QaWordCloud::getQuestionId, questionId)
                .eq(QaWordCloud::getStatus, 1)
                .orderByDesc(QaWordCloud::getGeneratedAt)
                .last("limit 1"));
        if (wordCloud == null) {
            return null;
        }
        return QaWordCloudVO.builder()
                .id(wordCloud.getId())
                .questionId(wordCloud.getQuestionId())
                .wordData(wordCloud.getWordData())
                .imageUrl(wordCloud.getImageUrl())
                .generatedAt(wordCloud.getGeneratedAt())
                .build();
    }

    private QaQuestion getVisibleQuestion(Long questionId) {
        QaQuestion question = qaQuestionMapper.selectById(questionId);
        if (question == null || Objects.equals(question.getStatus(), QUESTION_STATUS_DELETED)) {
            throw new QaException("问题不存在");
        }
        if (Objects.equals(question.getStatus(), QUESTION_STATUS_CLOSED)) {
            throw new QaException("问题已关闭");
        }
        return question;
    }

    private QaQuestion getOwnedQuestion(Long questionId, Long currentUserId) {
        QaQuestion question = qaQuestionMapper.selectById(questionId);
        if (question == null || Objects.equals(question.getStatus(), QUESTION_STATUS_DELETED)) {
            throw new QaException("问题不存在");
        }
        if (!Objects.equals(question.getUserId(), currentUserId) && !isAdmin()) {
            throw new QaException("仅提问者可操作问题");
        }
        return question;
    }

    private QaAnswer getOwnedAnswer(Long answerId, Long currentUserId) {
        QaAnswer answer = qaAnswerMapper.selectById(answerId);
        if (answer == null || Objects.equals(answer.getStatus(), ANSWER_STATUS_DELETED)) {
            throw new QaException("回答不存在");
        }
        if (!Objects.equals(answer.getUserId(), currentUserId) && !isAdmin()) {
            throw new QaException("仅回答作者可操作回答");
        }
        return answer;
    }

    private boolean isAdmin() {
        String roles = servletRequest.getHeader("X-User-Roles");
        if (!StringUtils.hasText(roles)) {
            return false;
        }
        return List.of(roles.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .anyMatch(role -> "ADMIN".equals(role)
                        || "ROLE_ADMIN".equals(role)
                        || "SUPER_ADMIN".equals(role)
                        || "MANAGER".equals(role));
    }

    private QaCategory getCategory(Long categoryId) {
        QaCategory category = qaCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getStatus(), 1)) {
            throw new QaException("问题分类不存在");
        }
        return category;
    }

    private void validateQuestionStatus(Integer status) {
        if (!Objects.equals(status, QUESTION_STATUS_UNSOLVED)
                && !Objects.equals(status, QUESTION_STATUS_SOLVED)
                && !Objects.equals(status, QUESTION_STATUS_CLOSED)) {
            throw new QaException("问题状态不合法");
        }
    }

    private void validateCommentTarget(Integer targetType, Long targetId) {
        if (!Objects.equals(targetType, TARGET_TYPE_QUESTION) && !Objects.equals(targetType, TARGET_TYPE_ANSWER)) {
            throw new QaException("评论目标类型不合法");
        }
        if (Objects.equals(targetType, TARGET_TYPE_QUESTION)) {
            getVisibleQuestion(targetId);
            return;
        }
        QaAnswer answer = qaAnswerMapper.selectById(targetId);
        if (answer == null || !Objects.equals(answer.getStatus(), ANSWER_STATUS_VISIBLE)) {
            throw new QaException("回答不存在");
        }
    }

    private void saveVote(Integer targetType, Long targetId, Integer voteType, QaAnswer answer) {
        if (!Objects.equals(voteType, VOTE_TYPE_LIKE) && !Objects.equals(voteType, VOTE_TYPE_DISLIKE)) {
            throw new QaException("投票类型不合法");
        }
        Long currentUserId = getCurrentUserId();
        QaVote existed = qaVoteMapper.selectOne(new LambdaQueryWrapper<QaVote>()
                .eq(QaVote::getTargetType, targetType)
                .eq(QaVote::getTargetId, targetId)
                .eq(QaVote::getUserId, currentUserId)
                .last("limit 1"));
        Integer oldVoteType = existed == null ? null : existed.getVoteType();
        if (existed != null && Objects.equals(oldVoteType, voteType)) {
            return;
        }
        if (existed != null) {
            existed.setVoteType(voteType);
            qaVoteMapper.updateById(existed);
        } else {
            QaVote vote = new QaVote();
            vote.setTargetType(targetType);
            vote.setTargetId(targetId);
            vote.setUserId(currentUserId);
            vote.setVoteType(voteType);
            vote.setCreatedAt(LocalDateTime.now());
            qaVoteMapper.insert(vote);
        }
        if (Objects.equals(targetType, TARGET_TYPE_ANSWER) && answer != null) {
            adjustAnswerVoteCount(answer.getId(), oldVoteType, voteType);
        }
    }

    private void adjustAnswerVoteCount(Long answerId, Integer oldVoteType, Integer newVoteType) {
        if (Objects.equals(oldVoteType, VOTE_TYPE_LIKE) && !Objects.equals(newVoteType, VOTE_TYPE_LIKE)) {
            qaAnswerMapper.update(null, new LambdaUpdateWrapper<QaAnswer>()
                    .eq(QaAnswer::getId, answerId)
                    .setSql("like_count = GREATEST(IFNULL(like_count, 0) - 1, 0)"));
        }
        if (Objects.equals(oldVoteType, VOTE_TYPE_DISLIKE) && !Objects.equals(newVoteType, VOTE_TYPE_DISLIKE)) {
            qaAnswerMapper.update(null, new LambdaUpdateWrapper<QaAnswer>()
                    .eq(QaAnswer::getId, answerId)
                    .setSql("dislike_count = GREATEST(IFNULL(dislike_count, 0) - 1, 0)"));
        }
        if (Objects.equals(newVoteType, VOTE_TYPE_LIKE) && !Objects.equals(oldVoteType, VOTE_TYPE_LIKE)) {
            qaAnswerMapper.update(null, new LambdaUpdateWrapper<QaAnswer>()
                    .eq(QaAnswer::getId, answerId)
                    .setSql("like_count = IFNULL(like_count, 0) + 1"));
        }
        if (Objects.equals(newVoteType, VOTE_TYPE_DISLIKE) && !Objects.equals(oldVoteType, VOTE_TYPE_DISLIKE)) {
            qaAnswerMapper.update(null, new LambdaUpdateWrapper<QaAnswer>()
                    .eq(QaAnswer::getId, answerId)
                    .setSql("dislike_count = IFNULL(dislike_count, 0) + 1"));
        }
    }

    private void applyQuestionSort(LambdaQueryWrapper<QaQuestion> wrapper, String sortBy) {
        if ("hot".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(QaQuestion::getFollowCount, QaQuestion::getAnswerCount, QaQuestion::getViewCount, QaQuestion::getCreatedAt);
            return;
        }
        if ("bounty".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(QaQuestion::getBountyPoints, QaQuestion::getCreatedAt);
            return;
        }
        wrapper.orderByDesc(QaQuestion::getCreatedAt);
    }

    private PageResponse<QaQuestionListItemVO> listQuestionPage(LambdaQueryWrapper<QaQuestion> wrapper, Long pageNum, Long pageSize) {
        long currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<QaQuestion> page = qaQuestionMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        return PageResponse.of(currentPage, currentSize, page.getTotal(), buildQuestionListItems(page.getRecords()));
    }

    private List<QaQuestionListItemVO> buildQuestionListItems(List<QaQuestion> questions) {
        if (questions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserProfileVO> userProfiles = loadUserProfiles(questions.stream().map(QaQuestion::getUserId).collect(Collectors.toSet()));
        Map<Long, String> categoryNames = loadCategoryNames(questions.stream().map(QaQuestion::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return questions.stream()
                .map(question -> {
                    UserProfileVO userProfile = userProfiles.get(question.getUserId());
                    return QaQuestionListItemVO.builder()
                            .id(question.getId())
                            .userId(question.getUserId())
                            .username(resolveUserName(userProfile, question.getUserId()))
                            .avatarUrl(resolveUserAvatar(userProfile))
                            .schoolName(resolveSchoolName(userProfile))
                            .companyName(resolveCompanyName(userProfile))
                            .authorTitle(resolveTitle(userProfile))
                            .authorIsVip(resolveAuthorIsVip(userProfile))
                            .categoryId(question.getCategoryId())
                            .categoryName(categoryNames.get(question.getCategoryId()))
                            .title(question.getTitle())
                            .bountyPoints(safeInt(question.getBountyPoints()))
                            .viewCount(safeInt(question.getViewCount()))
                            .answerCount(safeInt(question.getAnswerCount()))
                            .followCount(safeInt(question.getFollowCount()))
                            .status(safeInt(question.getStatus()))
                            .createdAt(question.getCreatedAt())
                            .build();
                })
                .toList();
    }

    private QaQuestionDetailVO toQuestionDetailVO(QaQuestion question, Long currentUserId) {
        UserProfileVO userProfile = fetchUserProfile(question.getUserId());
        Map<Long, String> categoryNames = question.getCategoryId() == null
                ? Collections.emptyMap()
                : loadCategoryNames(Set.of(question.getCategoryId()));
        List<QaAnswer> answers = qaAnswerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getQuestionId, question.getId())
                .eq(QaAnswer::getStatus, ANSWER_STATUS_VISIBLE)
                .orderByDesc(QaAnswer::getIsAccepted, QaAnswer::getLikeCount, QaAnswer::getCreatedAt));
        Map<Long, UserProfileVO> answerUsers = loadUserProfiles(answers.stream().map(QaAnswer::getUserId).collect(Collectors.toSet()));
        boolean followed = false;
        if (currentUserId != null) {
            followed = qaFollowMapper.selectCount(new LambdaQueryWrapper<QaFollow>()
                    .eq(QaFollow::getQuestionId, question.getId())
                    .eq(QaFollow::getUserId, currentUserId)) > 0;
        }
        return QaQuestionDetailVO.builder()
                .id(question.getId())
                .userId(question.getUserId())
                .username(resolveUserName(userProfile, question.getUserId()))
                .avatarUrl(resolveUserAvatar(userProfile))
                .schoolName(resolveSchoolName(userProfile))
                .companyName(resolveCompanyName(userProfile))
                .authorTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .categoryId(question.getCategoryId())
                .categoryName(categoryNames.get(question.getCategoryId()))
                .title(question.getTitle())
                .content(question.getContent())
                .bountyPoints(safeInt(question.getBountyPoints()))
                .viewCount(safeInt(question.getViewCount()))
                .answerCount(safeInt(question.getAnswerCount()))
                .followCount(safeInt(question.getFollowCount()))
                .shareCount(safeInt(question.getShareCount()))
                .status(safeInt(question.getStatus()))
                .bestAnswerId(question.getBestAnswerId())
                .followed(followed)
                .createdAt(question.getCreatedAt())
                .answers(answers.stream().map(answer -> toAnswerVO(answer, answerUsers)).toList())
                .build();
    }

    private QaAnswerVO toAnswerVO(QaAnswer answer, Map<Long, UserProfileVO> userProfiles) {
        UserProfileVO userProfile = userProfiles.get(answer.getUserId());
        return QaAnswerVO.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .userId(answer.getUserId())
                .username(resolveUserName(userProfile, answer.getUserId()))
                .avatarUrl(resolveUserAvatar(userProfile))
                .schoolName(resolveSchoolName(userProfile))
                .companyName(resolveCompanyName(userProfile))
                .authorTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .content(answer.getContent())
                .likeCount(safeInt(answer.getLikeCount()))
                .dislikeCount(safeInt(answer.getDislikeCount()))
                .isAccepted(safeInt(answer.getIsAccepted()))
                .createdAt(answer.getCreatedAt())
                .build();
    }

    private QaCommentVO toCommentVO(QaComment comment, Map<Long, UserProfileVO> userProfiles, Set<Long> likedCommentIds) {
        UserProfileVO userProfile = userProfiles.get(comment.getUserId());
        return QaCommentVO.builder()
                .id(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .userId(comment.getUserId())
                .username(resolveUserName(userProfile, comment.getUserId()))
                .avatarUrl(resolveUserAvatar(userProfile))
                .schoolName(resolveSchoolName(userProfile))
                .companyName(resolveCompanyName(userProfile))
                .authorTitle(resolveTitle(userProfile))
                .authorIsVip(resolveAuthorIsVip(userProfile))
                .parentId(comment.getParentId())
                .replyToUserId(comment.getReplyToUserId())
                .content(comment.getContent())
                .likeCount(safeInt(comment.getLikeCount()))
                .liked(likedCommentIds.contains(comment.getId()))
                .createdAt(comment.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }

    private void appendChildren(QaCommentVO parent, Map<Long, List<QaCommentVO>> childMap) {
        List<QaCommentVO> children = childMap.getOrDefault(parent.getId(), Collections.emptyList());
        parent.setChildren(children);
        children.forEach(child -> appendChildren(child, childMap));
    }

    private void increaseActivityLevel(Long userId) {
        ApiResponse<Void> response = userFeignClient.increaseActivityLevel(userId);
        if (response == null || response.getCode() == null || response.getCode() != 0) {
            throw new QaException(response == null ? "更新用户活跃度失败" : response.getMessage());
        }
    }

    private Map<Long, String> loadCategoryNames(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return qaCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(QaCategory::getId, QaCategory::getCategoryName, (a, b) -> a));
    }

    private Map<Long, UserProfileVO> loadUserProfiles(Set<Long> userIds) {
        Map<Long, UserProfileVO> result = new LinkedHashMap<>();
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            UserProfileVO userProfile = fetchUserProfile(userId);
            if (userProfile != null) {
                result.put(userId, userProfile);
            }
        }
        return result;
    }

    private UserProfileVO fetchUserProfile(Long userId) {
        try {
            ApiResponse<UserProfileVO> response = userFeignClient.getUserProfile(userId);
            return response == null ? null : response.getData();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveUserName(UserProfileVO userProfile, Long userId) {
        if (userProfile != null && StringUtils.hasText(userProfile.getNickname())) {
            return userProfile.getNickname();
        }
        if (userProfile != null && StringUtils.hasText(userProfile.getUsername())) {
            return userProfile.getUsername();
        }
        return "用户" + userId;
    }

    private String resolveUserAvatar(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getAvatarUrl();
    }

    private String resolveSchoolName(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getSchoolName();
    }

    private String resolveCompanyName(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getCompanyName();
    }

    private String resolveTitle(UserProfileVO userProfile) {
        return userProfile == null ? null : userProfile.getTitle();
    }

    private Integer resolveAuthorIsVip(UserProfileVO userProfile) {
        if (userProfile == null || !Objects.equals(userProfile.getIsVip(), 1)
                || userProfile.getVipExpiredAt() == null) {
            return 0;
        }
        return userProfile.getVipExpiredAt().isAfter(LocalDateTime.now()) ? 1 : 0;
    }

    private Long getCurrentUserId() {
        String userId = servletRequest.getHeader("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            throw new QaException("未登录或登录已失效");
        }
        return Long.parseLong(userId);
    }

    private Long getCurrentUserIdNullable() {
        String userId = servletRequest.getHeader("X-User-Id");
        return StringUtils.hasText(userId) ? Long.parseLong(userId) : null;
    }

    private boolean shouldSkipViewCount() {
        return "development".equalsIgnoreCase(servletRequest.getHeader("X-Client-Env"));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
