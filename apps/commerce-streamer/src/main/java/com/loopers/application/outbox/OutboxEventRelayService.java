package com.loopers.application.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Outbox Event를 Kafka로 발행하는 릴레이 서비스
 * - 스케줄러를 통해 주기적으로 PENDING 상태의 이벤트를 폴링
 * - Kafka로 발행 후 PUBLISHED 상태로 변경
 * - At Least Once 보장 (재시도 지원)
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxEventRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    private static final int BATCH_SIZE = 100;

    /**
     * 주기적으로 Outbox 이벤트를 Kafka로 발행
     * - 5초마다 실행
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents(BATCH_SIZE);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox 이벤트 발행 시작: count={}", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            relayEvent(event);
        }
    }

    /**
     * 단일 Outbox 이벤트를 Kafka로 발행
     */
    private void relayEvent(OutboxEvent event) {
        try {
            // Kafka로 발행 (partitionKey = aggregateId)
            CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(
                event.getTopic(),
                event.getAggregateId(), // 파티션 키
                event.getPayload()
            );

            // 비동기 처리 결과 핸들링
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    // 발행 성공
                    event.markAsPublished();
                    outboxEventRepository.save(event);
                    log.info("Kafka 발행 성공: topic={}, partition={}, offset={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    // 발행 실패
                    handlePublishFailure(event, ex);
                }
            });

        } catch (Exception e) {
            handlePublishFailure(event, e);
        }
    }

    /**
     * 발행 실패 처리
     */
    private void handlePublishFailure(OutboxEvent event, Throwable ex) {
        log.error("Kafka 발행 실패: eventType={}, aggregateId={}, error={}",
            event.getEventType(), event.getAggregateId(), ex.getMessage(), ex);

        if (event.canRetry()) {
            event.retry();
            log.info("재시도 예정: retryCount={}", event.getRetryCount());
        } else {
            event.markAsFailed(ex.getMessage());
            log.error("최대 재시도 횟수 초과: eventType={}, aggregateId={}",
                event.getEventType(), event.getAggregateId());
        }

        outboxEventRepository.save(event);
    }

    /**
     * 실패 이벤트 재시도
     * - 1분마다 실행
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    @Transactional
    public void retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxEventRepository.findRetryableFailedEvents(BATCH_SIZE);

        if (failedEvents.isEmpty()) {
            return;
        }

        log.info("실패 이벤트 재시도 시작: count={}", failedEvents.size());

        for (OutboxEvent event : failedEvents) {
            relayEvent(event);
        }
    }
}
