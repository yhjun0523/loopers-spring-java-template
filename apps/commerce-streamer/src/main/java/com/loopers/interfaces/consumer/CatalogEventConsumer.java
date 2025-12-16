package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.productmetrics.ProductMetricsService;
import com.loopers.domain.eventhandled.EventHandled;
import com.loopers.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catalog 이벤트 Consumer
 * - Topic: catalog-events
 * - 멱등 처리: event_handled 테이블 기반
 * - Metrics 집계: product_metrics 테이블 업데이트
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class CatalogEventConsumer {

    private final EventHandledRepository eventHandledRepository;
    private final ProductMetricsService productMetricsService;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "catalog-events";
    private static final String AGGREGATE_TYPE = "Product";

    /**
     * Catalog 이벤트 배치 리스너
     * - 멱등 처리: event_handled 테이블에서 중복 체크
     * - 수동 커밋: 모든 메시지 처리 성공 후 acknowledgment
     */
    @KafkaListener(
        topics = TOPIC,
        groupId = "commerce-streamer-catalog",
        containerFactory = "BATCH_LISTENER_DEFAULT"
    )
    public void consume(
        List<ConsumerRecord<String, String>> records,
        Acknowledgment acknowledgment
    ) {
        log.info("[Kafka Consumer] catalog-events 수신: count={}", records.size());

        for (ConsumerRecord<String, String> record : records) {
            try {
                processEvent(record);
            } catch (Exception e) {
                log.error("[Kafka Consumer] 이벤트 처리 실패: key={}, value={}, error={}",
                    record.key(), record.value(), e.getMessage(), e);
                // DLQ로 전송하거나 별도 처리 필요
                // 지금은 로그만 남기고 계속 진행
            }
        }

        // 모든 메시지 처리 완료 후 커밋
        acknowledgment.acknowledge();
        log.info("[Kafka Consumer] catalog-events 처리 완료 및 커밋: count={}", records.size());
    }

    /**
     * 단일 이벤트 처리
     */
    @Transactional
    public void processEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String payload = record.value();
        JsonNode eventNode = objectMapper.readTree(payload);

        String eventId = eventNode.get("eventId").asText();
        String eventType = eventNode.get("eventType").asText();
        Long productId = eventNode.get("productId").asLong();
        String aggregateId = productId.toString();

        // 멱등 처리: 이미 처리된 이벤트는 스킵
        if (isDuplicateEvent(eventId)) {
            log.info("[Kafka Consumer] 중복 이벤트 스킵: eventId={}, eventType={}", eventId, eventType);
            return;
        }

        log.info("[Kafka Consumer] 이벤트 처리 시작: eventId={}, eventType={}, productId={}",
            eventId, eventType, productId);

        // 이벤트 타입별 처리
        try {
            switch (eventType) {
                case "ProductLikeAdded" -> productMetricsService.incrementLikeCount(productId);
                case "ProductLikeRemoved" -> productMetricsService.decrementLikeCount(productId);
                case "ProductViewed" -> productMetricsService.incrementViewCount(productId);
                default -> log.warn("[Kafka Consumer] 알 수 없는 이벤트 타입: {}", eventType);
            }

            // 처리 성공 기록
            saveEventHandled(eventId, eventType, aggregateId);

            log.info("[Kafka Consumer] 이벤트 처리 완료: eventId={}, eventType={}", eventId, eventType);

        } catch (Exception e) {
            // 처리 실패 기록
            saveEventHandledFailure(eventId, eventType, aggregateId, e.getMessage());
            throw e;
        }
    }

    /**
     * 중복 이벤트 체크
     */
    private boolean isDuplicateEvent(String eventId) {
        return eventHandledRepository.existsByEventId(eventId);
    }

    /**
     * 처리 성공 기록
     */
    private void saveEventHandled(String eventId, String eventType, String aggregateId) {
        try {
            EventHandled eventHandled = EventHandled.createSuccess(
                eventId,
                eventType,
                AGGREGATE_TYPE,
                aggregateId
            );
            eventHandledRepository.save(eventHandled);
        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 중복 저장 시도 시 무시
            log.warn("[Kafka Consumer] 중복 이벤트 처리 기록 무시: eventId={}", eventId);
        }
    }

    /**
     * 처리 실패 기록
     */
    private void saveEventHandledFailure(String eventId, String eventType, String aggregateId, String errorMessage) {
        try {
            EventHandled eventHandled = EventHandled.createFailure(
                eventId,
                eventType,
                AGGREGATE_TYPE,
                aggregateId,
                errorMessage
            );
            eventHandledRepository.save(eventHandled);
        } catch (Exception e) {
            log.error("[Kafka Consumer] 실패 기록 저장 실패: eventId={}, error={}", eventId, e.getMessage());
        }
    }
}
