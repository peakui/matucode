package com.peakui.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.peakui.common.result.ApiResponse;
import com.peakui.common.result.PageResponse;
import com.peakui.oj.exception.OjException;
import com.peakui.oj.feign.AuthFeignClient;
import com.peakui.oj.feign.vo.UserBriefVO;
import com.peakui.oj.judge.JudgeStatusEnum;
import com.peakui.oj.mq.OjJudgeProducer;
import com.peakui.oj.mapper.ClassAssignmentMapper;
import com.peakui.oj.mapper.ClassDiscussionMapper;
import com.peakui.oj.mapper.ClassMemberMapper;
import com.peakui.oj.mapper.ClassSubmissionMapper;
import com.peakui.oj.mapper.OjClassMapper;
import com.peakui.oj.mapper.OjClassProblemRelationMapper;
import com.peakui.oj.mapper.OjProblemContentMapper;
import com.peakui.oj.mapper.OjProblemMapper;
import com.peakui.oj.mapper.OjSubmissionDetailMapper;
import com.peakui.oj.mapper.OjSubmissionMapper;
import com.peakui.oj.mapper.OjTestCaseMapper;
import com.peakui.oj.model.dto.*;
import com.peakui.oj.model.entity.*;
import com.peakui.oj.model.vo.*;
import com.peakui.oj.service.OjService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OjServiceImpl implements OjService {
    private static final int RELATION_CREATED = 1;
    private static final int RELATION_ASSOCIATED = 2;
    private static final int RELATION_FORKED = 3;
    private static final int RELATION_ACTIVE = 1;
    /** class_submissions.status：已完成/AC。 */
    private static final int CLASS_SUBMISSION_AC = 1;
    /** class_assignments.status：进行中，允许提交。 */
    private static final int ASSIGNMENT_STATUS_ONGOING = 1;
    /** 每通过一道作业题目计 100 分。 */
    private static final BigDecimal SCORE_PER_PROBLEM = new BigDecimal("100");
    private static final String DEFAULT_STARTER_CODE_JSON = "{\"cpp\": \"#include <iostream>\\nusing namespace std;\\n\\nint main() {\\n    int a, b;\\n    cin >> a >> b;\\n    // 在此处编写你的代码\\n    return 0;\\n}\", \"java\": \"import java.util.Scanner;\\n\\npublic class Main {\\n    public static void main(String[] args) {\\n        Scanner sc = new Scanner(System.in);\\n        int a = sc.nextInt();\\n        int b = sc.nextInt();\\n        // 在此处编写你的代码\\n    }\\n}\", \"python\": \"a, b = map(int, input().split())\\n# 在此处编写你的代码\"}";
    private final OjClassMapper ojClassMapper;
    private final ClassMemberMapper classMemberMapper;
    private final ClassAssignmentMapper classAssignmentMapper;
    private final ClassDiscussionMapper classDiscussionMapper;
    private final ClassSubmissionMapper classSubmissionMapper;
    private final AuthFeignClient authFeignClient;
    private final OjClassProblemRelationMapper ojClassProblemRelationMapper;
    private final OjProblemMapper ojProblemMapper;
    private final OjProblemContentMapper ojProblemContentMapper;
    private final OjTestCaseMapper ojTestCaseMapper;
    private final OjSubmissionDetailMapper ojSubmissionDetailMapper;
    private final OjSubmissionMapper ojSubmissionMapper;
    private final OjJudgeProducer ojJudgeProducer;
    private final HttpServletRequest request;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassVO createClass(CreateClassRequest request) {
        Long uid = currentUserId();
        OjClass c = new OjClass();
        c.setName(request.getName().trim());
        c.setType(validClassType(request.getType()));
        c.setDescription(text(request.getDescription()));
        c.setCreatorId(uid);
        c.setCoverImage(text(request.getCoverImage()));
        c.setJoinMode(validJoinMode(request.getJoinMode()));
        c.setInviteCode(
                Objects.equals(c.getJoinMode(), 3)
                        ? (StringUtils.hasText(request.getInviteCode())
                        ? request.getInviteCode().trim()
                        : UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                        : null
        );
        c.setStatus(1);
        c.setStartTime(request.getStartTime());
        c.setEndTime(request.getEndTime());
        c.setCreatedAt(LocalDateTime.now());
        ojClassMapper.insert(c);

        ClassMember m = new ClassMember();
        m.setClassId(c.getId());
        m.setUserId(uid);
        m.setRole(1);
        m.setJoinStatus(1);
        m.setJoinedAt(LocalDateTime.now());
        classMemberMapper.insert(m);
        log.info("创建 OJ 班级成功, classId={}, name={}, creatorId={}", c.getId(), c.getName(), uid);
        return classVO(c, uid);
    }

    @Override
    public ClassVO updateClass(Long classId, UpdateClassRequest request) {
        OjClass c = ownedClass(classId);
        c.setName(request.getName().trim());
        if (request.getType() != null) {
            c.setType(validClassType(request.getType()));
        }
        c.setDescription(text(request.getDescription()));
        c.setCoverImage(text(request.getCoverImage()));
        if (request.getJoinMode() != null) {
            c.setJoinMode(validJoinMode(request.getJoinMode()));
        }
        if (Objects.equals(c.getJoinMode(), 3)) {
            c.setInviteCode(StringUtils.hasText(request.getInviteCode()) ? request.getInviteCode().trim() : c.getInviteCode());
        } else {
            c.setInviteCode(null);
        }
        if (request.getStatus() != null) {
            c.setStatus(request.getStatus());
        }
        if (request.getStartTime() != null) {
            c.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            c.setEndTime(request.getEndTime());
        }
        ojClassMapper.updateById(c);
        log.info("更新 OJ 班级成功, classId={}, operatorId={}", classId, currentUserIdNullable());
        return classVO(c, currentUserIdNullable());
    }

    @Override
    public ClassVO getClassDetail(Long classId) {
        return classVO(classById(classId), currentUserIdNullable());
    }

    @Override
    public PageResponse<ClassVO> listClasses(String keyword, Integer status, Long pageNum, Long pageSize) {
        long p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long s = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<OjClass> w = new LambdaQueryWrapper<>();
        w.eq(OjClass::getStatus, status == null ? 1 : status)
                .orderByDesc(OjClass::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            w.and(x -> x.like(OjClass::getName, keyword).or().like(OjClass::getDescription, keyword));
        }
        Page<OjClass> page = ojClassMapper.selectPage(new Page<>(p, s), w);
        Long uid = currentUserIdNullable();
        return PageResponse.of(p, s, page.getTotal(), page.getRecords().stream().map(it -> classVO(it, uid)).toList());
    }

    @Override
    public void joinClass(Long classId, JoinClassRequest request) {
        OjClass c = classById(classId);
        Long uid = currentUserId();
        ClassMember m = classMemberMapper.selectOne(
                new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getClassId, classId)
                        .eq(ClassMember::getUserId, uid)
                        .last("limit 1")
        );
        int st = Objects.equals(c.getJoinMode(), 1)
                ? 1
                : Objects.equals(c.getJoinMode(), 2)
                ? 0
                : inviteOk(c, request.getInviteCode());
        if (m == null) {
            m = new ClassMember();
            m.setClassId(classId);
            m.setUserId(uid);
            m.setRole(3);
            m.setJoinStatus(st);
            m.setJoinedAt(LocalDateTime.now());
            classMemberMapper.insert(m);
            log.info("申请加入 OJ 班级, classId={}, userId={}, joinStatus={}", classId, uid, st);
            return;
        }
        if (Objects.equals(m.getJoinStatus(), 1)) {
            return;
        }
        m.setJoinStatus(st);
        m.setRole(3);
        m.setJoinedAt(LocalDateTime.now());
        classMemberMapper.updateById(m);
        log.info("重新申请加入 OJ 班级, classId={}, userId={}, joinStatus={}", classId, uid, st);
    }

    @Override
    public void approveMember(Long classId, Long memberId) {
        ownedClass(classId);
        ClassMember member = member(classId, memberId);
        member.setJoinStatus(1);
        classMemberMapper.updateById(member);
        log.info("通过班级成员申请, classId={}, memberId={}, operatorId={}",
                classId, memberId, currentUserIdNullable());
    }
    @Override
    public void rejectMember(Long classId, Long memberId) {
        ownedClass(classId);
        ClassMember member = member(classId, memberId);
        if (Objects.equals(member.getRole(), 1)) {
            throw new OjException("不能拒绝班级创建者");
        }
        member.setJoinStatus(2);
        classMemberMapper.updateById(member);
        log.info("拒绝班级成员申请, classId={}, memberId={}, operatorId={}",
                classId, memberId, currentUserIdNullable());
    }
    @Override
    public void removeMember(Long classId, Long memberId) {
        ownedClass(classId);
        ClassMember member = member(classId, memberId);
        if (Objects.equals(member.getRole(), 1)) {
            throw new OjException("不能移除班级创建者");
        }
        member.setJoinStatus(3);
        classMemberMapper.updateById(member);
        log.info("移除班级成员, classId={}, memberId={}, operatorId={}",
                classId, memberId, currentUserIdNullable());
    }
    @Override
    public List<ClassMemberVO> listMembers(Long classId) {
        joined(classId);
        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .orderByAsc(ClassMember::getRole, ClassMember::getJoinedAt));
        Map<Long, UserBriefVO> users = loadUserBriefs(members.stream().map(ClassMember::getUserId).toList());
        return members.stream().map(member -> memberVO(member, users)).toList();
    }

    @Override
    public ClassAssignmentVO createAssignment(Long classId, CreateAssignmentRequest request) {
        teacher(classId);
        ClassAssignment assignment = new ClassAssignment();
        assignment.setClassId(classId);
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(text(request.getDescription()));
        assignment.setType(request.getType());
        assignment.setProblemIds(request.getProblemIds() == null ? "[]" : request.getProblemIds().toString());
        assignment.setStartTime(request.getStartTime());
        assignment.setDeadline(request.getDeadline());
        assignment.setMaxAttempts(request.getMaxAttempts() == null ? -1 : request.getMaxAttempts());
        assignment.setIsPublicRank(request.getIsPublicRank() == null ? 0 : request.getIsPublicRank());
        assignment.setStatus(1);
        assignment.setCreatedBy(currentUserId());
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        classAssignmentMapper.insert(assignment);
        log.info("创建班级作业成功, classId={}, assignmentId={}, title={}",
                classId, assignment.getId(), assignment.getTitle());
        return assignmentVO(assignment);
    }

    @Override
    public ClassAssignmentVO updateAssignment(Long classId, Long assignmentId, UpdateAssignmentRequest request) {
        teacher(classId);
        ClassAssignment assignment = assignment(classId, assignmentId);
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(text(request.getDescription()));
        if (request.getType() != null) {
            assignment.setType(request.getType());
        }
        if (request.getProblemIds() != null) {
            assignment.setProblemIds(request.getProblemIds().toString());
        }
        assignment.setStartTime(request.getStartTime());
        assignment.setDeadline(request.getDeadline());
        assignment.setMaxAttempts(request.getMaxAttempts() == null ? -1 : request.getMaxAttempts());
        assignment.setIsPublicRank(request.getIsPublicRank() == null ? 0 : request.getIsPublicRank());
        if (request.getStatus() != null) {
            assignment.setStatus(request.getStatus());
        }
        assignment.setUpdatedAt(LocalDateTime.now());
        classAssignmentMapper.updateById(assignment);
        log.info("更新班级作业成功, classId={}, assignmentId={}", classId, assignmentId);
        return assignmentVO(assignment);
    }

    @Override
    public List<ClassAssignmentVO> listAssignments(Long classId) {
        joined(classId);
        return classAssignmentMapper.selectList(new LambdaQueryWrapper<ClassAssignment>()
                        .eq(ClassAssignment::getClassId, classId)
                        .orderByDesc(ClassAssignment::getCreatedAt))
                .stream()
                .map(this::assignmentVO)
                .toList();
    }

    @Override
    public AssignmentDetailVO getAssignmentDetail(Long classId, Long assignmentId) {
        joined(classId);
        ClassAssignment assignment = assignment(classId, assignmentId);
        Long uid = currentUserId();
        Set<Long> pool = loadClassProblemIds(classId);
        List<Long> problemIds = parseProblemIds(assignment.getProblemIds()).stream()
                .filter(pool::contains)
                .distinct()
                .toList();

        Map<Long, OjProblem> problems = problemIds.isEmpty() ? Collections.emptyMap()
                : ojProblemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(OjProblem::getId, problem -> problem, (a, b) -> a));

        Map<Long, ClassSubmission> myClassSubmissions = classSubmissionMapper.selectList(
                        new LambdaQueryWrapper<ClassSubmission>()
                                .eq(ClassSubmission::getAssignmentId, assignmentId)
                                .eq(ClassSubmission::getUserId, uid))
                .stream()
                .collect(Collectors.toMap(ClassSubmission::getProblemId, sub -> sub, (a, b) -> a));

        Map<Long, List<OjSubmission>> myAttempts = ojSubmissionMapper.selectList(
                        new LambdaQueryWrapper<OjSubmission>()
                                .eq(OjSubmission::getAssignmentId, assignmentId)
                                .eq(OjSubmission::getUserId, uid)
                                .orderByDesc(OjSubmission::getCreatedAt))
                .stream()
                .collect(Collectors.groupingBy(OjSubmission::getProblemId));

        List<AssignmentProblemVO> items = problemIds.stream().map(problemId -> {
            OjProblem problem = problems.get(problemId);
            ClassSubmission classSubmission = myClassSubmissions.get(problemId);
            List<OjSubmission> attempts = myAttempts.getOrDefault(problemId, Collections.emptyList());
            return AssignmentProblemVO.builder()
                    .problemId(problemId)
                    .problemNo(problem == null ? null : problem.getProblemNo())
                    .title(problem == null ? null : problem.getTitle())
                    .difficulty(problem == null ? null : problem.getDifficulty())
                    .solved(classSubmission != null
                            && Objects.equals(classSubmission.getStatus(), CLASS_SUBMISSION_AC))
                    .attemptsUsed(attempts.size())
                    .latestSubmissionId(attempts.isEmpty() ? null : attempts.get(0).getId())
                    .build();
        }).toList();

        return AssignmentDetailVO.builder()
                .id(assignment.getId())
                .classId(assignment.getClassId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .type(assignment.getType())
                .startTime(assignment.getStartTime())
                .deadline(assignment.getDeadline())
                .maxAttempts(assignment.getMaxAttempts())
                .isPublicRank(assignment.getIsPublicRank())
                .status(assignment.getStatus())
                .submittable(isSubmittable(assignment))
                .problems(items)
                .build();
    }

    @Override
    public AssignmentRankingVO getAssignmentRanking(Long classId, Long assignmentId) {
        ClassAssignment assignment = assignment(classId, assignmentId);
        if (!isTeacherOrAdmin(classId)) {
            joined(classId);
            if (!Objects.equals(assignment.getIsPublicRank(), 1)) {
                throw new OjException("排行榜未公开");
            }
        }
        List<Long> problemIds = parseProblemIds(assignment.getProblemIds());

        List<ClassSubmission> submissions = classSubmissionMapper.selectList(
                new LambdaQueryWrapper<ClassSubmission>()
                        .eq(ClassSubmission::getAssignmentId, assignmentId)
                        .eq(ClassSubmission::getStatus, CLASS_SUBMISSION_AC));
        Map<Long, List<ClassSubmission>> byUser = submissions.stream()
                .collect(Collectors.groupingBy(ClassSubmission::getUserId));

        List<ClassMember> members = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getJoinStatus, 1));
        LinkedHashSet<Long> userIds = new LinkedHashSet<>(members.stream()
                .map(ClassMember::getUserId).toList());
        userIds.addAll(byUser.keySet());
        Map<Long, UserBriefVO> users = loadUserBriefs(userIds);

        List<AssignmentRankingVO.RankRow> rows = userIds.stream().map(userId -> {
            List<ClassSubmission> mine = byUser.getOrDefault(userId, Collections.emptyList());
            int solved = (int) mine.stream().map(ClassSubmission::getProblemId).distinct().count();
            BigDecimal score = mine.stream().map(ClassSubmission::getScore).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDateTime lastSubmitAt = mine.stream().map(ClassSubmission::getSubmittedAt)
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
            UserBriefVO user = users.get(userId);
            return AssignmentRankingVO.RankRow.builder()
                    .userId(userId)
                    .nickname(user == null ? null : user.getNickname())
                    .avatarUrl(user == null ? null : user.getAvatarUrl())
                    .solvedCount(solved)
                    .score(score)
                    .lastSubmitAt(lastSubmitAt)
                    .build();
        }).sorted(Comparator
                        .comparingInt(AssignmentRankingVO.RankRow::getSolvedCount).reversed()
                        .thenComparing(AssignmentRankingVO.RankRow::getScore, Comparator.reverseOrder())
                        .thenComparing(AssignmentRankingVO.RankRow::getLastSubmitAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<AssignmentRankingVO.RankRow> ranked = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            AssignmentRankingVO.RankRow row = rows.get(i);
            row.setRank(i + 1);
            ranked.add(row);
        }

        return AssignmentRankingVO.builder()
                .assignmentId(assignmentId)
                .totalProblems(problemIds.size())
                .isPublicRank(assignment.getIsPublicRank())
                .rows(ranked)
                .build();
    }

    @Override
    public OjClassRankingVO getClassRanking(Long classId) {
        OjClass clazz = classById(classId);
        joined(classId);

        Set<Long> problemIdSet = loadClassProblemIds(classId);
        List<Long> problemIds = new ArrayList<>(problemIdSet);
        Map<Long, OjProblem> problemMap = problemIds.isEmpty() ? Collections.emptyMap()
                : ojProblemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(OjProblem::getId, problem -> problem, (a, b) -> a));

        List<OjClassRankingVO.ProblemBrief> problems = new ArrayList<>();
        int index = 0;
        for (Long problemId : problemIds) {
            OjProblem problem = problemMap.get(problemId);
            if (problem == null) {
                continue;
            }
            problems.add(OjClassRankingVO.ProblemBrief.builder()
                    .problemId(problemId)
                    .problemNo(problem.getProblemNo())
                    .title(problem.getTitle())
                    .index(++index)
                    .build());
        }

        List<Long> memberIds = classMemberMapper.selectList(new LambdaQueryWrapper<ClassMember>()
                        .eq(ClassMember::getClassId, classId)
                        .eq(ClassMember::getJoinStatus, 1))
                .stream()
                .map(ClassMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<OjSubmission> submissions = Collections.emptyList();
        if (!memberIds.isEmpty() && !problemIds.isEmpty()) {
            LambdaQueryWrapper<OjSubmission> wrapper = new LambdaQueryWrapper<OjSubmission>()
                    .in(OjSubmission::getProblemId, problemIds)
                    .in(OjSubmission::getUserId, memberIds);
            if (clazz.getStartTime() != null) {
                wrapper.ge(OjSubmission::getCreatedAt, clazz.getStartTime());
            }
            if (clazz.getEndTime() != null) {
                wrapper.le(OjSubmission::getCreatedAt, clazz.getEndTime());
            }
            wrapper.orderByAsc(OjSubmission::getCreatedAt, OjSubmission::getId);
            submissions = ojSubmissionMapper.selectList(wrapper);
        }

        // 罚时基准：优先竞赛开始时间；未设置时退化为该场最早提交时间。
        LocalDateTime base = clazz.getStartTime();
        if (base == null) {
            base = submissions.stream()
                    .map(OjSubmission::getCreatedAt)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
        }

        // userId -> problemId -> 单题作答累计
        Map<Long, Map<Long, ProblemAcc>> accByUser = new HashMap<>();
        for (OjSubmission submission : submissions) {
            if (submission.getProblemId() == null || submission.getUserId() == null) {
                continue;
            }
            ProblemAcc acc = accByUser
                    .computeIfAbsent(submission.getUserId(), k -> new HashMap<>())
                    .computeIfAbsent(submission.getProblemId(), k -> new ProblemAcc());
            if (Objects.equals(submission.getStatus(), JudgeStatusEnum.ACCEPTED.getCode())) {
                if (acc.acAt == null) {
                    acc.acAt = submission.getCreatedAt();
                }
            } else if (acc.acAt == null && isCountedWrong(submission.getStatus())) {
                acc.wrong++;
            }
        }

        Map<Long, UserBriefVO> users = loadUserBriefs(memberIds);
        List<OjClassRankingVO.Row> rows = new ArrayList<>();
        for (Long userId : memberIds) {
            Map<Long, ProblemAcc> byProblem = accByUser.getOrDefault(userId, Collections.emptyMap());
            List<OjClassRankingVO.Cell> cells = new ArrayList<>();
            int solved = 0;
            long penalty = 0L;
            LocalDateTime lastAcAt = null;
            for (OjClassRankingVO.ProblemBrief problem : problems) {
                ProblemAcc acc = byProblem.get(problem.getProblemId());
                boolean isSolved = acc != null && acc.acAt != null;
                int wrong = acc == null ? 0 : acc.wrong;
                Long acMinutes = null;
                if (isSolved) {
                    solved++;
                    acMinutes = penaltyMinutes(base, acc.acAt);
                    penalty += acMinutes + 20L * wrong;
                    if (lastAcAt == null || acc.acAt.isAfter(lastAcAt)) {
                        lastAcAt = acc.acAt;
                    }
                }
                cells.add(OjClassRankingVO.Cell.builder()
                        .problemId(problem.getProblemId())
                        .solved(isSolved)
                        .wrongAttempts(wrong)
                        .acMinutes(acMinutes)
                        .acAt(acc == null ? null : acc.acAt)
                        .build());
            }
            UserBriefVO user = users.get(userId);
            rows.add(OjClassRankingVO.Row.builder()
                    .userId(userId)
                    .userName(displayName(user, userId))
                    .avatarUrl(user == null ? null : user.getAvatarUrl())
                    .solvedCount(solved)
                    .penaltyMinutes(penalty)
                    .lastAcAt(lastAcAt)
                    .cells(cells)
                    .build());
        }

        rows.sort(Comparator
                .comparingInt(OjClassRankingVO.Row::getSolvedCount).reversed()
                .thenComparingLong(OjClassRankingVO.Row::getPenaltyMinutes)
                .thenComparing(OjClassRankingVO.Row::getLastAcAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(OjClassRankingVO.Row::getUserId));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(i + 1);
        }

        return OjClassRankingVO.builder()
                .classId(classId)
                .name(clazz.getName())
                .startTime(clazz.getStartTime())
                .endTime(clazz.getEndTime())
                .memberCount(memberIds.size())
                .problems(problems)
                .rows(rows)
                .build();
    }

    /** 计入罚时的错误提交：非 AC、非等待、非编译错误。 */
    private boolean isCountedWrong(Integer status) {
        if (status == null) {
            return false;
        }
        return !Objects.equals(status, JudgeStatusEnum.ACCEPTED.getCode())
                && !Objects.equals(status, JudgeStatusEnum.WAITING.getCode())
                && !Objects.equals(status, JudgeStatusEnum.COMPILE_ERROR.getCode());
    }

    private long penaltyMinutes(LocalDateTime base, LocalDateTime acAt) {
        if (base == null || acAt == null || acAt.isBefore(base)) {
            return 0L;
        }
        return Duration.between(base, acAt).toMinutes();
    }

    private String displayName(UserBriefVO user, Long userId) {
        if (user != null) {
            if (StringUtils.hasText(user.getNickname())) {
                return user.getNickname();
            }
            if (StringUtils.hasText(user.getUsername())) {
                return user.getUsername();
            }
        }
        return "用户 " + userId;
    }

    /** 单个用户单题的作答累计。 */
    private static final class ProblemAcc {
        private LocalDateTime acAt;
        private int wrong;
    }

    @Override
    public PageResponse<ClassSubmissionVO> listAssignmentSubmissions(Long classId, Long assignmentId,
                                                                    Long userId, Integer status,
                                                                    Long pageNum, Long pageSize) {
        teacher(classId);
        ClassAssignment assignment = assignment(classId, assignmentId);
        long p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long s = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<ClassSubmission> wrapper = new LambdaQueryWrapper<ClassSubmission>()
                .eq(ClassSubmission::getAssignmentId, assignmentId);
        if (userId != null) {
            wrapper.eq(ClassSubmission::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(ClassSubmission::getStatus, status);
        }
        wrapper.orderByDesc(ClassSubmission::getSubmittedAt, ClassSubmission::getId);
        Page<ClassSubmission> page = classSubmissionMapper.selectPage(new Page<>(p, s), wrapper);

        List<Long> problemIds = parseProblemIds(assignment.getProblemIds());
        Map<Long, String> problemTitles = problemIds.isEmpty() ? Collections.emptyMap()
                : ojProblemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(OjProblem::getId, OjProblem::getTitle, (a, b) -> a));
        Map<Long, UserBriefVO> users = loadUserBriefs(page.getRecords().stream()
                .map(ClassSubmission::getUserId).toList());

        return PageResponse.of(p, s, page.getTotal(), page.getRecords().stream().map(sub -> {
            UserBriefVO user = users.get(sub.getUserId());
            return ClassSubmissionVO.builder()
                    .id(sub.getId())
                    .assignmentId(sub.getAssignmentId())
                    .userId(sub.getUserId())
                    .nickname(user == null ? null : user.getNickname())
                    .problemId(sub.getProblemId())
                    .problemTitle(problemTitles.get(sub.getProblemId()))
                    .submissionId(sub.getSubmissionId())
                    .status(sub.getStatus())
                    .score(sub.getScore())
                    .submittedAt(sub.getSubmittedAt())
                    .build();
        }).toList());
    }

    @Override
    public ClassDiscussionVO createDiscussion(Long classId, CreateDiscussionRequest request) {
        joined(classId);
        ClassDiscussion discussion = new ClassDiscussion();
        discussion.setClassId(classId);
        discussion.setAssignmentId(request.getAssignmentId());
        discussion.setUserId(currentUserId());
        discussion.setTitle(request.getTitle().trim());
        discussion.setContent(request.getContent().trim());
        discussion.setIsAnonymous(request.getIsAnonymous() == null ? 0 : request.getIsAnonymous());
        discussion.setCreatedAt(LocalDateTime.now());
        classDiscussionMapper.insert(discussion);
        log.info("发表班级讨论成功, classId={}, discussionId={}, userId={}",
                classId, discussion.getId(), discussion.getUserId());
        return discussionVO(discussion);
    }

    @Override
    public List<ClassDiscussionVO> listDiscussions(Long classId, Long assignmentId) {
        joined(classId);
        LambdaQueryWrapper<ClassDiscussion> wrapper = new LambdaQueryWrapper<ClassDiscussion>()
                .eq(ClassDiscussion::getClassId, classId)
                .orderByDesc(ClassDiscussion::getCreatedAt);
        if (assignmentId != null) {
            wrapper.eq(ClassDiscussion::getAssignmentId, assignmentId);
        }
        return classDiscussionMapper.selectList(wrapper).stream().map(this::discussionVO).toList();
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OjSubmissionVO submitOjCode(SubmitOjCodeRequest request) {
        Long userId = currentUserId();
        Long assignmentId = request.getAssignmentId();
        if (assignmentId != null) {
            ClassAssignment assignment = classAssignmentMapper.selectById(assignmentId);
            if (assignment == null) {
                throw new OjException("作业不存在");
            }
            validateAssignmentSubmission(assignment, request.getProblemId(), userId);
        }
        OjSubmission submission = new OjSubmission();
        submission.setProblemId(request.getProblemId());
        submission.setUserId(userId);
        submission.setAssignmentId(assignmentId);
        submission.setLanguage(request.getLanguage().trim());
        submission.setCode(request.getCode());
        submission.setCodeLength(request.getCode().length());
        submission.setStatus(JudgeStatusEnum.WAITING.getCode());
        submission.setExecutionTime(0);
        submission.setMemoryUsed(0);
        submission.setPassedCases(0);
        submission.setTotalCases(0);
        submission.setPassRate(BigDecimal.ZERO);
        submission.setCreatedAt(LocalDateTime.now());
        ojSubmissionMapper.insert(submission);
        Long submissionId = submission.getId();
        log.info("提交 OJ 代码, submissionId={}, problemId={}, userId={}, language={}",
                submissionId, submission.getProblemId(), submission.getUserId(), submission.getLanguage());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    ojJudgeProducer.sendJudgeTask(submissionId);
                } catch (Exception e) {
                    log.error("判题任务入队失败, submissionId={}", submissionId, e);
                    submission.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getCode());
                    submission.setErrorMessage("判题队列发送失败");
                    submission.setJudgeTime(LocalDateTime.now());
                    ojSubmissionMapper.updateById(submission);
                }
            }
        });
        return submissionVO(submission);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OjProblemVO createProblem(CreateOjProblemRequest request) {
        OjProblem problem = new OjProblem();
        problem.setProblemNo(request.getProblemNo().trim());
        problem.setTitle(request.getTitle().trim());
        problem.setDescription(request.getDescription().trim());
        problem.setInputFormat(text(request.getInputFormat()));
        problem.setOutputFormat(text(request.getOutputFormat()));
        problem.setSampleInput(text(request.getSampleInput()));
        problem.setSampleOutput(text(request.getSampleOutput()));
        problem.setHint(text(request.getHint()));
        problem.setDifficulty(request.getDifficulty());
        problem.setCategoryId(request.getCategoryId());
        problem.setTimeLimit(request.getTimeLimit() == null ? 1000 : request.getTimeLimit());
        problem.setMemoryLimit(request.getMemoryLimit() == null ? 256 : request.getMemoryLimit());
        problem.setSubmitCount(0);
        problem.setAcceptCount(0);
        problem.setAcceptRate(BigDecimal.ZERO);
        problem.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        problem.setCreatedAt(LocalDateTime.now());
        problem.setUpdatedAt(LocalDateTime.now());
        ojProblemMapper.insert(problem);

        OjProblemContent content = new OjProblemContent();
        content.setProblemId(problem.getId());
        content.setContentMd(text(request.getContentMd()));
        content.setContentHtml(text(request.getContentHtml()));
        content.setStarterCodeJson(defaultStarterCodeJson(request.getStarterCodeJson()));
        content.setSolutionJson(text(request.getSolutionJson()));
        content.setTagsJson(request.getTags() == null ? "[]" : request.getTags().toString());
        content.setVersion(1);
        ojProblemContentMapper.insert(content);
        log.info("创建 OJ 题目成功, problemId={}, problemNo={}, title={}",
                problem.getId(), problem.getProblemNo(), problem.getTitle());
        return problemVO(problem, content);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OjProblemVO updateProblem(Long problemId, UpdateOjProblemRequest request) {
        OjProblem problem = problem(problemId);
        if (StringUtils.hasText(request.getProblemNo())) {
            problem.setProblemNo(request.getProblemNo().trim());
        }
        problem.setTitle(request.getTitle().trim());
        problem.setDescription(request.getDescription().trim());
        if (request.getDifficulty() != null) {
            problem.setDifficulty(request.getDifficulty());
        }
        if (request.getCategoryId() != null) {
            problem.setCategoryId(request.getCategoryId());
        }
        if (request.getTimeLimit() != null) {
            problem.setTimeLimit(request.getTimeLimit());
        }
        if (request.getMemoryLimit() != null) {
            problem.setMemoryLimit(request.getMemoryLimit());
        }
        if (request.getStatus() != null) {
            problem.setStatus(request.getStatus());
        }
        problem.setInputFormat(text(request.getInputFormat()));
        problem.setOutputFormat(text(request.getOutputFormat()));
        problem.setSampleInput(text(request.getSampleInput()));
        problem.setSampleOutput(text(request.getSampleOutput()));
        problem.setHint(text(request.getHint()));
        problem.setUpdatedAt(LocalDateTime.now());
        ojProblemMapper.updateById(problem);

        OjProblemContent content = ojProblemContentMapper.selectOne(
                new LambdaQueryWrapper<OjProblemContent>()
                        .eq(OjProblemContent::getProblemId, problemId)
                        .last("limit 1")
        );

        if (content == null) {
            content = new OjProblemContent();
            content.setProblemId(problemId);
            content.setContentMd(text(request.getContentMd()));
            content.setContentHtml(text(request.getContentHtml()));
            content.setStarterCodeJson(text(request.getStarterCodeJson()));
            content.setSolutionJson(text(request.getSolutionJson()));
            content.setTagsJson(request.getTags() == null ? "[]" : request.getTags().toString());
            content.setVersion(1);
            ojProblemContentMapper.insert(content);
            log.info("更新 OJ 题目成功（新建内容）, problemId={}", problemId);
            return problemVO(problem, content);
        }

        content.setContentMd(text(request.getContentMd()));
        content.setContentHtml(text(request.getContentHtml()));
        content.setStarterCodeJson(text(request.getStarterCodeJson()));
        content.setSolutionJson(text(request.getSolutionJson()));
        content.setTagsJson(request.getTags() == null ? "[]" : request.getTags().toString());
        content.setVersion(content.getVersion() == null ? 1 : content.getVersion() + 1);
        ojProblemContentMapper.updateById(content);
        log.info("更新 OJ 题目成功, problemId={}, contentVersion={}", problemId, content.getVersion());
        return problemVO(problem, content);
    }
    @Override
    public OjProblemVO getProblemDetail(Long problemId) {
        OjProblem problem = problem(problemId);
        ensureVisibleInDefaultOj(problem);
        OjProblemContent content = ojProblemContentMapper.selectOne(new LambdaQueryWrapper<OjProblemContent>()
                .eq(OjProblemContent::getProblemId, problemId)
                .last("limit 1"));
        return problemVO(problem, content);
    }
    @Override
    public PageResponse<OjProblemVO> listProblems(String keyword, Integer difficulty, Integer status, Long pageNum, Long pageSize) {
        long p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long s = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<OjProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(OjProblem::getCategoryId, 2);
        if (difficulty != null) {
            wrapper.eq(OjProblem::getDifficulty, difficulty);
        }
        if (status != null) {
            wrapper.eq(OjProblem::getStatus, status);
        } else {
            wrapper.eq(OjProblem::getStatus, 1);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(x -> x.like(OjProblem::getTitle, keyword).or().like(OjProblem::getProblemNo, keyword));
        }
        wrapper.orderByDesc(OjProblem::getCreatedAt);
        Page<OjProblem> page = ojProblemMapper.selectPage(new Page<>(p, s), wrapper);
        return PageResponse.of(p, s, page.getTotal(), page.getRecords().stream().map(item -> problemVO(item, null)).toList());
    }

    @Override
    public PageResponse<OjProblemVO> listClassProblems(Long classId, String keyword, Integer difficulty, Integer status, Long pageNum, Long pageSize) {
        joined(classId);
        Set<Long> problemIds = loadClassProblemIds(classId);
        long p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long s = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        if (problemIds.isEmpty()) {
            return PageResponse.of(p, s, 0, Collections.emptyList());
        }

        LambdaQueryWrapper<OjProblem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OjProblem::getId, problemIds);
        if (difficulty != null) {
            wrapper.eq(OjProblem::getDifficulty, difficulty);
        }
        if (status != null) {
            wrapper.eq(OjProblem::getStatus, status);
        } else {
            wrapper.eq(OjProblem::getStatus, 1);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(x -> x.like(OjProblem::getTitle, keyword).or().like(OjProblem::getProblemNo, keyword));
        }
        wrapper.orderByDesc(OjProblem::getCreatedAt);
        Page<OjProblem> page = ojProblemMapper.selectPage(new Page<>(p, s), wrapper);
        return PageResponse.of(p, s, page.getTotal(), page.getRecords().stream().map(item -> problemVO(item, null)).toList());
    }

    @Override
    public OjProblemVO getClassProblemDetail(Long classId, Long problemId) {
        joined(classId);
        classProblemRelation(classId, problemId);
        OjProblem problem = problem(problemId);
        OjProblemContent content = ojProblemContentMapper.selectOne(new LambdaQueryWrapper<OjProblemContent>()
                .eq(OjProblemContent::getProblemId, problemId)
                .last("limit 1"));
        return problemVO(problem, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OjProblemVO createClassProblem(Long classId, CreateOjProblemRequest request) {
        teacher(classId);
        OjProblemVO vo = createProblem(request);
        OjProblem problem = problem(vo.getId());
        problem.setCategoryId(2L);
        ojProblemMapper.updateById(problem);
        createClassProblemRelation(classId, problem.getId(), null, RELATION_CREATED);
        OjProblemContent content = ojProblemContentMapper.selectOne(new LambdaQueryWrapper<OjProblemContent>()
                .eq(OjProblemContent::getProblemId, problem.getId())
                .last("limit 1"));
        log.info("创建班级专属题目成功, classId={}, problemId={}", classId, problem.getId());
        return problemVO(problem, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OjProblemVO updateClassProblem(Long classId, Long problemId, UpdateOjProblemRequest request) {
        teacher(classId);
        OjClassProblemRelation relation = classProblemRelation(classId, problemId);
        Long targetProblemId = problemId;
        if (Objects.equals(relation.getRelationType(), RELATION_ASSOCIATED)) {
            targetProblemId = forkClassProblem(relation);
        }
        return updateProblem(targetProblemId, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void associateClassProblems(Long classId, AssociateOjProblemsRequest request) {
        teacher(classId);
        int linked = 0;
        for (Long problemId : request.getProblemIds()) {
            if (problemId == null) {
                continue;
            }
            problem(problemId);
            OjClassProblemRelation existing = findClassProblemRelation(classId, problemId);
            if (existing != null) {
                if (!Objects.equals(existing.getStatus(), RELATION_ACTIVE)) {
                    existing.setStatus(RELATION_ACTIVE);
                    existing.setUpdatedAt(LocalDateTime.now());
                    ojClassProblemRelationMapper.updateById(existing);
                }
                continue;
            }
            createClassProblemRelation(classId, problemId, null, RELATION_ASSOCIATED);
            linked++;
        }
        log.info("关联班级题目成功, classId={}, 新增关联={}", classId, linked);
    }

    @Override
    public void removeClassProblem(Long classId, Long problemId) {
        teacher(classId);
        OjClassProblemRelation relation = classProblemRelation(classId, problemId);
        relation.setStatus(0);
        relation.setUpdatedAt(LocalDateTime.now());
        ojClassProblemRelationMapper.updateById(relation);
        log.info("移除班级题目, classId={}, problemId={}", classId, problemId);
    }

    @Override
    public OjTestCaseVO addTestCase(Long problemId, CreateOjTestCaseRequest request) {
        problem(problemId);
        requireTestCaseManager(problemId);
        OjTestCase testCase = new OjTestCase();
        testCase.setProblemId(problemId);
        testCase.setCaseNo(request.getCaseNo());
        testCase.setInput(request.getInput());
        testCase.setExpectedOutput(request.getExpectedOutput());
        testCase.setIsSample(request.getIsSample() == null ? 0 : request.getIsSample());
        testCase.setScoreWeight(request.getScoreWeight() == null ? BigDecimal.ONE : request.getScoreWeight());
        testCase.setIsHidden(request.getIsHidden() == null ? 0 : request.getIsHidden());
        testCase.setCreatedAt(LocalDateTime.now());
        ojTestCaseMapper.insert(testCase);
        log.info("新增测试用例, problemId={}, caseId={}, caseNo={}, hidden={}",
                problemId, testCase.getId(), testCase.getCaseNo(), testCase.getIsHidden());
        return testCaseVO(testCase);
    }

    @Override
    public List<OjTestCaseVO> listTestCases(Long problemId) {
        problem(problemId);
        boolean admin = isAdmin();
        return ojTestCaseMapper.selectList(new LambdaQueryWrapper<OjTestCase>()
                        .eq(OjTestCase::getProblemId, problemId)
                        // 隐藏测试点是判题答案，仅管理员可见；其他用户只看得到样例。
                        .ne(!admin, OjTestCase::getIsHidden, 1)
                        .orderByAsc(OjTestCase::getCaseNo))
                .stream()
                .map(this::testCaseVO)
                .toList();
    }
    @Override
    public PageResponse<OjSubmissionVO> listSubmissions(Long problemId, Long userId, Integer status, Long pageNum, Long pageSize) {
        long p = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long s = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<OjSubmission> wrapper = new LambdaQueryWrapper<>();
        if (problemId != null) {
            wrapper.eq(OjSubmission::getProblemId, problemId);
        }
        if (userId != null) {
            wrapper.eq(OjSubmission::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(OjSubmission::getStatus, status);
        }
        wrapper.orderByDesc(OjSubmission::getCreatedAt);
        Page<OjSubmission> page = ojSubmissionMapper.selectPage(new Page<>(p, s), wrapper);
        List<OjSubmission> records = page.getRecords();
        Map<Long, UserBriefVO> users = loadUserBriefs(records.stream().map(OjSubmission::getUserId).toList());
        return PageResponse.of(p, s, page.getTotal(),
                records.stream().map(sub -> submissionVO(sub, users.get(sub.getUserId()))).toList());
    }

    @Override
    public OjSolvedProblemCountVO getSolvedProblemCount(Long userId) {
        Long targetUserId = resolveTargetUserId(userId);
        Integer count = ojSubmissionMapper.countDistinctProblemsByUserId(targetUserId, JudgeStatusEnum.ACCEPTED.getCode());
        return OjSolvedProblemCountVO.builder()
                .userId(targetUserId)
                .solvedProblemCount(count == null ? 0 : count)
                .build();
    }

    @Override
    public OjSubmissionVO getSubmissionDetail(Long submissionId) {
        OjSubmission submission = submission(submissionId);
        Map<Long, UserBriefVO> users = loadUserBriefs(Collections.singletonList(submission.getUserId()));
        return submissionVO(submission, users.get(submission.getUserId()));
    }
    @Override
    public List<OjSubmissionDetailVO> listSubmissionDetails(Long submissionId) {
        submission(submissionId);
        return ojSubmissionDetailMapper.selectList(new LambdaQueryWrapper<OjSubmissionDetail>()
                        .eq(OjSubmissionDetail::getSubmissionId, submissionId)
                        .orderByAsc(OjSubmissionDetail::getCaseNo))
                .stream()
                .map(this::submissionDetailVO)
                .toList();
    }

    private OjClass classById(Long id) {
        OjClass clazz = ojClassMapper.selectById(id);
        if (clazz == null || Objects.equals(clazz.getStatus(), 3)) {
            throw new OjException("班级不存在");
        }
        return clazz;
    }
    private OjClass ownedClass(Long id) {
        OjClass clazz = classById(id);
        if (!Objects.equals(clazz.getCreatorId(), currentUserId()) && !isAdmin()) {
            throw new OjException("无权操作该班级");
        }
        return clazz;
    }
    private ClassMember member(Long classId, Long memberId) {
        ClassMember member = classMemberMapper.selectById(memberId);
        if (member == null || !Objects.equals(member.getClassId(), classId)) {
            throw new OjException("班级成员不存在");
        }
        return member;
    }
    private ClassAssignment assignment(Long classId, Long id) {
        ClassAssignment assignment = classAssignmentMapper.selectById(id);
        if (assignment == null || !Objects.equals(assignment.getClassId(), classId)) {
            throw new OjException("作业不存在");
        }
        return assignment;
    }
    private void teacher(Long classId) {
        classById(classId);
        if (isAdmin()) {
            return;
        }
        ClassMember member = classMemberMapper.selectOne(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, currentUserId())
                .eq(ClassMember::getJoinStatus, 1)
                .last("limit 1"));
        if (member == null || (!Objects.equals(member.getRole(), 1) && !Objects.equals(member.getRole(), 2))) {
            throw new OjException("无教师权限");
        }
    }
    private boolean isAdmin() {
        String roles = request.getHeader("X-User-Roles");
        if (StringUtils.hasText(roles)) {
            boolean hasAdminRole = List.of(roles.split(",")).stream()
                    .map(String::trim)
                    .anyMatch("ADMIN"::equalsIgnoreCase);
            if (hasAdminRole) {
                return true;
            }
        }
        Long currentUserId = currentUserIdNullable();
        return Objects.equals(currentUserId, 1L);
    }
    /** 测试用例是判题答案：仅管理员，或该题目所挂班级的教师可管理。 */
    private void requireTestCaseManager(Long problemId) {
        if (isAdmin()) {
            return;
        }
        Long uid = currentUserIdNullable();
        if (uid != null) {
            List<Long> classIds = ojClassProblemRelationMapper.selectList(new LambdaQueryWrapper<OjClassProblemRelation>()
                            .eq(OjClassProblemRelation::getProblemId, problemId))
                    .stream()
                    .map(OjClassProblemRelation::getClassId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!classIds.isEmpty() && classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                    .in(ClassMember::getClassId, classIds)
                    .eq(ClassMember::getUserId, uid)
                    .eq(ClassMember::getJoinStatus, 1)
                    .in(ClassMember::getRole, List.of(1, 2))) > 0) {
                return;
            }
        }
        throw new OjException("无权管理该题目测试用例");
    }

    private void joined(Long classId) {
        classById(classId);
        if (isAdmin()) {
            return;
        }
        long count = classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, currentUserId())
                .eq(ClassMember::getJoinStatus, 1));
        if (count == 0) {
            throw new OjException("请先加入班级");
        }
    }

    /** 管理员，或该班级已通过的教师（role 1/2）。 */
    private boolean isTeacherOrAdmin(Long classId) {
        if (isAdmin()) {
            return true;
        }
        Long uid = currentUserIdNullable();
        if (uid == null) {
            return false;
        }
        return classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, classId)
                .eq(ClassMember::getUserId, uid)
                .eq(ClassMember::getJoinStatus, 1)
                .in(ClassMember::getRole, List.of(1, 2))) > 0;
    }

    /** 作业进行中且当前处于可提交时间窗内。 */
    private boolean isSubmittable(ClassAssignment assignment) {
        if (!Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_ONGOING)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getStartTime() != null && now.isBefore(assignment.getStartTime())) {
            return false;
        }
        return assignment.getDeadline() == null || !now.isAfter(assignment.getDeadline());
    }

    /** 校验作业提交：成员、状态、时间窗、题目归属、剩余次数。 */
    private void validateAssignmentSubmission(ClassAssignment assignment, Long problemId, Long userId) {
        joined(assignment.getClassId());
        if (!Objects.equals(assignment.getStatus(), ASSIGNMENT_STATUS_ONGOING)) {
            throw new OjException("作业未开始或已结束");
        }
        LocalDateTime now = LocalDateTime.now();
        if (assignment.getStartTime() != null && now.isBefore(assignment.getStartTime())) {
            throw new OjException("作业尚未开始");
        }
        if (assignment.getDeadline() != null && now.isAfter(assignment.getDeadline())) {
            throw new OjException("作业已截止");
        }
        if (!parseProblemIds(assignment.getProblemIds()).contains(problemId)) {
            throw new OjException("该题目不属于本次作业");
        }
        Integer maxAttempts = assignment.getMaxAttempts();
        if (maxAttempts != null && maxAttempts > 0) {
            long used = ojSubmissionMapper.selectCount(new LambdaQueryWrapper<OjSubmission>()
                    .eq(OjSubmission::getAssignmentId, assignment.getId())
                    .eq(OjSubmission::getUserId, userId)
                    .eq(OjSubmission::getProblemId, problemId));
            if (used >= maxAttempts) {
                throw new OjException("该题目提交次数已用完");
            }
        }
    }

    /** 批量查询用户简要信息，auth 不可用时降级为空 map，不影响主流程。 */
    private Map<Long, UserBriefVO> loadUserBriefs(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            ApiResponse<List<UserBriefVO>> response = authFeignClient.getUserBriefs(distinct);
            List<UserBriefVO> briefs = response == null ? null : response.getData();
            if (briefs == null || briefs.isEmpty()) {
                return Collections.emptyMap();
            }
            return briefs.stream()
                    .filter(brief -> brief.getUserId() != null)
                    .collect(Collectors.toMap(UserBriefVO::getUserId, brief -> brief, (a, b) -> a));
        } catch (Exception e) {
            log.warn("批量查询用户信息失败, ids={}", distinct, e);
            return Collections.emptyMap();
        }
    }

    private Set<Long> loadClassProblemIds(Long classId) {
        try {
            Set<Long> problemIds = ojClassProblemRelationMapper.selectList(new LambdaQueryWrapper<OjClassProblemRelation>()
                            .eq(OjClassProblemRelation::getClassId, classId)
                            .eq(OjClassProblemRelation::getStatus, RELATION_ACTIVE)
                            .orderByDesc(OjClassProblemRelation::getCreatedAt))
                    .stream()
                    .map(OjClassProblemRelation::getProblemId)
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
            return problemIds.isEmpty() ? loadAssignedProblemIds(classId) : problemIds;
        } catch (Exception ignored) {
            return loadAssignedProblemIds(classId);
        }
    }

    private OjClassProblemRelation classProblemRelation(Long classId, Long problemId) {
        OjClassProblemRelation relation = findClassProblemRelation(classId, problemId);
        if (relation == null || !Objects.equals(relation.getStatus(), RELATION_ACTIVE)) {
            throw new OjException("题目未关联到当前班级或竞赛");
        }
        return relation;
    }

    private OjClassProblemRelation findClassProblemRelation(Long classId, Long problemId) {
        return ojClassProblemRelationMapper.selectOne(new LambdaQueryWrapper<OjClassProblemRelation>()
                .eq(OjClassProblemRelation::getClassId, classId)
                .eq(OjClassProblemRelation::getProblemId, problemId)
                .last("limit 1"));
    }

    private OjClassProblemRelation createClassProblemRelation(Long classId, Long problemId, Long baseProblemId, Integer relationType) {
        OjClassProblemRelation relation = new OjClassProblemRelation();
        relation.setClassId(classId);
        relation.setProblemId(problemId);
        relation.setBaseProblemId(baseProblemId);
        relation.setRelationType(relationType);
        relation.setStatus(RELATION_ACTIVE);
        relation.setCreatedBy(currentUserId());
        relation.setCreatedAt(LocalDateTime.now());
        relation.setUpdatedAt(LocalDateTime.now());
        ojClassProblemRelationMapper.insert(relation);
        return relation;
    }

    private Long forkClassProblem(OjClassProblemRelation relation) {
        Long originalProblemId = relation.getProblemId();
        OjProblem original = problem(originalProblemId);
        OjProblem copy = copyProblem(original);
        ojProblemMapper.insert(copy);
        copyProblemContent(originalProblemId, copy.getId());
        copyTestCases(originalProblemId, copy.getId());
        relation.setProblemId(copy.getId());
        relation.setBaseProblemId(originalProblemId);
        relation.setRelationType(RELATION_FORKED);
        relation.setUpdatedAt(LocalDateTime.now());
        ojClassProblemRelationMapper.updateById(relation);
        log.info("班级关联题目已分叉为独立副本, classId={}, baseProblemId={}, newProblemId={}",
                relation.getClassId(), originalProblemId, copy.getId());
        return copy.getId();
    }

    private OjProblem copyProblem(OjProblem original) {
        OjProblem copy = new OjProblem();
        copy.setProblemNo(original.getProblemNo());
        copy.setTitle(original.getTitle());
        copy.setDescription(original.getDescription());
        copy.setInputFormat(original.getInputFormat());
        copy.setOutputFormat(original.getOutputFormat());
        copy.setSampleInput(original.getSampleInput());
        copy.setSampleOutput(original.getSampleOutput());
        copy.setHint(original.getHint());
        copy.setDifficulty(original.getDifficulty());
        copy.setCategoryId(2L);
        copy.setTimeLimit(original.getTimeLimit());
        copy.setMemoryLimit(original.getMemoryLimit());
        copy.setSubmitCount(0);
        copy.setAcceptCount(0);
        copy.setAcceptRate(BigDecimal.ZERO);
        copy.setStatus(original.getStatus());
        copy.setCreatedAt(LocalDateTime.now());
        copy.setUpdatedAt(LocalDateTime.now());
        return copy;
    }

    private void copyProblemContent(Long originalProblemId, Long newProblemId) {
        OjProblemContent originalContent = ojProblemContentMapper.selectOne(new LambdaQueryWrapper<OjProblemContent>()
                .eq(OjProblemContent::getProblemId, originalProblemId)
                .last("limit 1"));
        if (originalContent == null) {
            return;
        }
        OjProblemContent copy = new OjProblemContent();
        copy.setProblemId(newProblemId);
        copy.setContentMd(originalContent.getContentMd());
        copy.setContentHtml(originalContent.getContentHtml());
        copy.setStarterCodeJson(originalContent.getStarterCodeJson());
        copy.setSolutionJson(originalContent.getSolutionJson());
        copy.setTagsJson(originalContent.getTagsJson());
        copy.setVersion(1);
        ojProblemContentMapper.insert(copy);
    }

    private void copyTestCases(Long originalProblemId, Long newProblemId) {
        List<OjTestCase> testCases = ojTestCaseMapper.selectList(new LambdaQueryWrapper<OjTestCase>()
                .eq(OjTestCase::getProblemId, originalProblemId));
        for (OjTestCase testCase : testCases) {
            OjTestCase copy = new OjTestCase();
            copy.setProblemId(newProblemId);
            copy.setCaseNo(testCase.getCaseNo());
            copy.setInput(testCase.getInput());
            copy.setExpectedOutput(testCase.getExpectedOutput());
            copy.setIsSample(testCase.getIsSample());
            copy.setScoreWeight(testCase.getScoreWeight());
            copy.setIsHidden(testCase.getIsHidden());
            copy.setCreatedAt(LocalDateTime.now());
            ojTestCaseMapper.insert(copy);
        }
    }

    private Set<Long> loadAssignedProblemIds(Long classId) {
        List<ClassAssignment> assignments = classAssignmentMapper.selectList(
                new LambdaQueryWrapper<ClassAssignment>()
                        .eq(ClassAssignment::getClassId, classId)
                        .eq(ClassAssignment::getStatus, 1)
        );
        Set<Long> problemIds = new LinkedHashSet<>();
        for (ClassAssignment assignment : assignments) {
            problemIds.addAll(parseProblemIds(assignment.getProblemIds()));
        }
        return problemIds;
    }

    private List<Long> parseProblemIds(String problemIdsText) {
        if (!StringUtils.hasText(problemIdsText)) {
            return Collections.emptyList();
        }
        String trimmed = problemIdsText.trim();
        if (trimmed.length() < 2) {
            return Collections.emptyList();
        }
        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }
        String[] parts = content.split(",");
        List<Long> result = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private int inviteOk(OjClass clazz, String code) {
        if (!StringUtils.hasText(code) || !Objects.equals(clazz.getInviteCode(), code.trim())) {
            throw new OjException("邀请码错误");
        }
        return 1;
    }

    private String defaultStarterCodeJson(String starterCodeJson) {
        return StringUtils.hasText(starterCodeJson) ? starterCodeJson.trim() : DEFAULT_STARTER_CODE_JSON;
    }
    private int validClassType(Integer value) {
        if (!List.of(1, 2).contains(value)) {
            throw new OjException("班级类型不合法");
        }
        return value;
    }
    private int validJoinMode(Integer value) {
        if (!List.of(1, 2, 3).contains(value)) {
            throw new OjException("加入方式不合法");
        }
        return value;
    }
    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
    private ClassVO classVO(OjClass clazz, Long uid) {
        boolean joined = uid != null && classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, clazz.getId())
                .eq(ClassMember::getUserId, uid)
                .eq(ClassMember::getJoinStatus, 1)) > 0;
        int count = Math.toIntExact(classMemberMapper.selectCount(new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getClassId, clazz.getId())
                .eq(ClassMember::getJoinStatus, 1)));
        return ClassVO.builder()
                .id(clazz.getId())
                .name(clazz.getName())
                .type(clazz.getType())
                .description(clazz.getDescription())
                .creatorId(clazz.getCreatorId())
                .coverImage(clazz.getCoverImage())
                .joinMode(clazz.getJoinMode())
                .inviteCode(clazz.getInviteCode())
                .status(clazz.getStatus())
                .joined(joined)
                .memberCount(count)
                .startTime(clazz.getStartTime())
                .endTime(clazz.getEndTime())
                .createdAt(clazz.getCreatedAt())
                .build();
    }
    private ClassMemberVO memberVO(ClassMember member, Map<Long, UserBriefVO> users) {
        UserBriefVO user = users.get(member.getUserId());
        return ClassMemberVO.builder()
                .id(member.getId())
                .classId(member.getClassId())
                .userId(member.getUserId())
                .nickname(user == null ? null : user.getNickname())
                .avatarUrl(user == null ? null : user.getAvatarUrl())
                .role(member.getRole())
                .joinStatus(member.getJoinStatus())
                .joinedAt(member.getJoinedAt())
                .build();
    }
    private ClassAssignmentVO assignmentVO(ClassAssignment assignment) {
        return ClassAssignmentVO.builder()
                .id(assignment.getId())
                .classId(assignment.getClassId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .type(assignment.getType())
                .problemIds(parseProblemIds(assignment.getProblemIds()))
                .startTime(assignment.getStartTime())
                .deadline(assignment.getDeadline())
                .maxAttempts(assignment.getMaxAttempts())
                .isPublicRank(assignment.getIsPublicRank())
                .status(assignment.getStatus())
                .createdBy(assignment.getCreatedBy())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }
    private OjProblem problem(Long id) {
        OjProblem problem = ojProblemMapper.selectById(id);
        if (problem == null) {
            throw new OjException("题目不存在");
        }
        return problem;
    }
    private void ensureVisibleInDefaultOj(OjProblem problem) {
        if (Objects.equals(problem.getCategoryId(), 2L) || Objects.equals(problem.getCategoryId(), 2)) {
            throw new OjException("该题目仅在班级/刷题场景下可见");
        }
    }
    private void ensureVisibleInClassOj(OjProblem problem) {
        if (!Objects.equals(problem.getCategoryId(), 2L) && !Objects.equals(problem.getCategoryId(), 2)) {
            throw new OjException("该题目不属于班级/刷题场景");
        }
    }
    private OjSubmission submission(Long id) {
        OjSubmission submission = ojSubmissionMapper.selectById(id);
        if (submission == null) {
            throw new OjException("提交记录不存在");
        }
        return submission;
    }
    private OjProblemContent content(Long problemId) {
        OjProblemContent content = ojProblemContentMapper.selectOne(new LambdaQueryWrapper<OjProblemContent>()
                .eq(OjProblemContent::getProblemId, problemId)
                .last("limit 1"));
        if (content == null) {
            throw new OjException("题目内容不存在");
        }
        return content;
    }
    private OjProblemVO problemVO(OjProblem problem, OjProblemContent content) {
        return OjProblemVO.builder()
                .id(problem.getId())
                .problemNo(problem.getProblemNo())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .inputFormat(problem.getInputFormat())
                .outputFormat(problem.getOutputFormat())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .hint(problem.getHint())
                .difficulty(problem.getDifficulty())
                .categoryId(problem.getCategoryId())
                .timeLimit(problem.getTimeLimit())
                .memoryLimit(problem.getMemoryLimit())
                .submitCount(problem.getSubmitCount())
                .acceptCount(problem.getAcceptCount())
                .acceptRate(problem.getAcceptRate())
                .status(problem.getStatus())
                .contentMd(content == null ? null : content.getContentMd())
                .contentHtml(content == null ? null : content.getContentHtml())
                .starterCodeJson(content == null ? null : content.getStarterCodeJson())
                .solutionJson(content == null ? null : content.getSolutionJson())
                .tags(Collections.emptyList())
                .createdAt(problem.getCreatedAt())
                .updatedAt(problem.getUpdatedAt())
                .build();
    }
    private OjSubmissionVO submissionVO(OjSubmission submission) {
        return submissionVO(submission, null);
    }

    private OjSubmissionVO submissionVO(OjSubmission submission, UserBriefVO user) {
        return OjSubmissionVO.builder()
                .id(submission.getId())
                .problemId(submission.getProblemId())
                .userId(submission.getUserId())
                .username(user == null ? null : user.getUsername())
                .nickname(user == null ? null : user.getNickname())
                .avatarUrl(user == null ? null : user.getAvatarUrl())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .executionTime(submission.getExecutionTime())
                .memoryUsed(submission.getMemoryUsed())
                .passRate(submission.getPassRate())
                .passedCases(submission.getPassedCases())
                .totalCases(submission.getTotalCases())
                .errorMessage(submission.getErrorMessage())
                .judgeTime(submission.getJudgeTime())
                .createdAt(submission.getCreatedAt())
                .build();
    }
    private OjSubmissionDetailVO submissionDetailVO(OjSubmissionDetail detail) {
        return OjSubmissionDetailVO.builder()
                .id(detail.getId())
                .submissionId(detail.getSubmissionId())
                .caseNo(detail.getCaseNo())
                .input(detail.getInput())
                .expectedOutput(detail.getExpectedOutput())
                .actualOutput(detail.getActualOutput())
                .status(detail.getStatus())
                .executionTime(detail.getExecutionTime())
                .memoryUsed(detail.getMemoryUsed())
                .errorMessage(detail.getErrorMessage())
                .createdAt(detail.getCreatedAt())
                .build();
    }
    private OjTestCaseVO testCaseVO(OjTestCase testCase) {
        return OjTestCaseVO.builder()
                .id(testCase.getId())
                .problemId(testCase.getProblemId())
                .caseNo(testCase.getCaseNo())
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .isSample(testCase.getIsSample())
                .scoreWeight(testCase.getScoreWeight())
                .isHidden(testCase.getIsHidden())
                .createdAt(testCase.getCreatedAt())
                .build();
    }
    private ClassDiscussionVO discussionVO(ClassDiscussion discussion) {
        return ClassDiscussionVO.builder()
                .id(discussion.getId())
                .classId(discussion.getClassId())
                .assignmentId(discussion.getAssignmentId())
                .userId(discussion.getUserId())
                .title(discussion.getTitle())
                .content(discussion.getContent())
                .isAnonymous(discussion.getIsAnonymous())
                .createdAt(discussion.getCreatedAt())
                .build();
    }
    private Long resolveTargetUserId(Long userId) {
        Long currentUserId = currentUserId();
        Long targetUserId = userId == null ? currentUserId : userId;
        if (!Objects.equals(targetUserId, currentUserId) && !isAdmin()) {
            throw new OjException("无权查看该用户统计");
        }
        return targetUserId;
    }

    private Long currentUserId() {
        String uid = request.getHeader("X-User-Id");
        if (!StringUtils.hasText(uid)) {
            throw new OjException("未获取到用户信息");
        }
        return Long.parseLong(uid);
    }

    private Long currentUserIdNullable() {
        String uid = request.getHeader("X-User-Id");
        return StringUtils.hasText(uid) ? Long.parseLong(uid) : null;
    }
}
