package com.loopers.domain.eventhandled;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Transactional Inbox Pattern 구현을 위한 이벤트 처리 기록 엔티티
 * - Consumer → Broker 의 At Most Once 보장 (멱등 처리)
 * - 중복 메시지 방어 및 처리 이력 관리
 */
@Entity
@Table(
    name = "event_handled",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_id", columnNames = "eventId")
    },
    indexes = {
        @Index(name = "idx_aggregate", columnList = "aggregateType,aggregateId"),
        @Index(name = "idx_handled_at", columnList = "handledAt")
    }
)
public class EventHandled extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventHandleStatus status;

    @Column(nullable = false)
    private LocalDateTime handledAt;

    @Column(length = 500)
    private String errorMessage;

    protected EventHandled() {}

    private EventHandled(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId
    ) {
        validateEventId(eventId);
        validateEventType(eventType);
        validateAggregateType(aggregateType);
        validateAggregateId(aggregateId);

        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.status = EventHandleStatus.SUCCESS;
        this.handledAt = LocalDateTime.now();
    }

    /**
     * 성공 처리 기록 생성 팩토리 메서드
     */
    public static EventHandled createSuccess(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId
    ) {
        return new EventHandled(eventId, eventType, aggregateType, aggregateId);
    }

    /**
     * 실패 처리 기록 생성 팩토리 메서드
     */
    public static EventHandled createFailure(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String errorMessage
    ) {
        EventHandled eventHandled = new EventHandled(eventId, eventType, aggregateType, aggregateId);
        eventHandled.status = EventHandleStatus.FAILED;
        eventHandled.errorMessage = errorMessage;
        return eventHandled;
    }

    // Getters
    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public EventHandleStatus getStatus() {
        return status;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // 검증 메서드들
    private void validateEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "eventId는 비어있을 수 없습니다");
        }
    }

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
}
