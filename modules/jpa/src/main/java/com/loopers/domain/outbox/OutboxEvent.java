package com.loopers.domain.outbox;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Transactional Outbox Pattern 구현을 위한 이벤트 저장 엔티티
 * - Producer → Broker 의 At Least Once 보장
 * - 도메인 변경과 이벤트 발행을 하나의 트랜잭션으로 묶음
 */
@Entity
@Table(
    name = "outbox_event",
    indexes = {
        @Index(name = "idx_status_created", columnList = "status,createdAt"),
        @Index(name = "idx_aggregate", columnList = "aggregateType,aggregateId")
    }
)
public class OutboxEvent extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false, length = 100)
    private String topic;

    private LocalDateTime publishedAt;

    private Integer retryCount;

    @Column(length = 500)
    private String errorMessage;

    protected OutboxEvent() {}

    private OutboxEvent(
        String eventType,
        String aggregateType,
        String aggregateId,
        String payload,
        String topic
    ) {
        validateEventType(eventType);
        validateAggregateType(aggregateType);
        validateAggregateId(aggregateId);
        validatePayload(payload);
        validateTopic(topic);

        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.topic = topic;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    /**
     * Outbox 이벤트 생성 팩토리 메서드
     */
    public static OutboxEvent create(
        String eventType,
        String aggregateType,
        String aggregateId,
        String payload,
        String topic
    ) {
        return new OutboxEvent(eventType, aggregateType, aggregateId, payload, topic);
    }

    /**
     * 발행 성공 처리
     */
    public void markAsPublished() {
        if (this.status == OutboxStatus.PUBLISHED) {
            return; // 멱등성
        }
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 발행 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = OutboxStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * 재시도 처리
     */
    public void retry() {
        if (this.status == OutboxStatus.PUBLISHED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 발행된 이벤트는 재시도할 수 없습니다");
        }
        this.status = OutboxStatus.PENDING;
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < 5; // 최대 5회 재시도
    }

    // Getters
    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public String getTopic() {
        return topic;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // 검증 메서드들
    private void validateEventType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "eventType은 비어있을 수 없습니다");
        }
    }

    private void validateAggregateType(String aggregateType) {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "aggregateType은 비어있을 수 없습니다");
        }
    }

    private void validateAggregateId(String aggregateId) {
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "aggregateId는 비어있을 수 없습니다");
        }
    }

    private void validatePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "payload는 비어있을 수 없습니다");
        }
    }

    private void validateTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "topic은 비어있을 수 없습니다");
        }
    }
}
