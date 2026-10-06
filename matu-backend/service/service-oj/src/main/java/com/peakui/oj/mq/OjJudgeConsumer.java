package com.peakui.oj.mq;

import com.peakui.oj.exception.OjException;
import com.peakui.oj.judge.OjJudgeService;
import com.peakui.oj.mapper.OjSubmissionMapper;
import com.peakui.oj.model.entity.OjSubmission;
import com.peakui.oj.judge.JudgeStatusEnum;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OjJudgeConsumer {
    private static final String JUDGE_LOCK_KEY_PREFIX = "oj:judge:lock:";
    private static final int MAX_RETRY_TIMES = 3;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    private final OjJudgeService ojJudgeService;
    private final OjSubmissionMapper ojSubmissionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConfig.JUDGE_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String payload = new String(message.getBody(), StandardCharsets.UTF_8).trim();
        if (!StringUtils.hasText(payload)) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        Long submissionId;
        try {
            submissionId = Long.parseLong(payload);
        } catch (NumberFormatException e) {
            log.warn("判题消息格式错误, payload={}", payload);
            channel.basicAck(deliveryTag, false);
            return;
        }

        String lockKey = JUDGE_LOCK_KEY_PREFIX + submissionId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            channel.basicAck(deliveryTag, false);
            return;
        }

        try {
            for (int attempt = 1; attempt <= MAX_RETRY_TIMES; attempt++) {
                try {
                    ojJudgeService.doJudge(submissionId);
                    channel.basicAck(deliveryTag, false);
                    return;
                } catch (OjException e) {
                    markSubmissionSystemError(submissionId, e.getMessage());
                    channel.basicAck(deliveryTag, false);
                    return;
                } catch (Exception e) {
                    if (attempt == MAX_RETRY_TIMES) {
                        log.error("判题消息处理失败, submissionId={}, attempts={}", submissionId, attempt, e);
                        markSubmissionSystemError(submissionId, "判题处理失败");
                        channel.basicNack(deliveryTag, false, false);
                        return;
                    }
                    log.warn("判题消息重试, submissionId={}, attempt={}", submissionId, attempt, e);
                }
            }
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void markSubmissionSystemError(Long submissionId, String message) {
        OjSubmission submission = ojSubmissionMapper.selectById(submissionId);
        if (submission == null || !Objects.equals(submission.getStatus(), JudgeStatusEnum.WAITING.getCode())) {
            return;
        }
        submission.setStatus(JudgeStatusEnum.SYSTEM_ERROR.getCode());
        submission.setExecutionTime(0);
        submission.setMemoryUsed(0);
        submission.setPassedCases(0);
        submission.setTotalCases(0);
        submission.setErrorMessage(StringUtils.hasText(message) ? message : JudgeStatusEnum.SYSTEM_ERROR.getMessage());
        submission.setJudgeTime(LocalDateTime.now());
        ojSubmissionMapper.updateById(submission);
    }
}
