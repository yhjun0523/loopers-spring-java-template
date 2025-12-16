package com.loopers.domain.event;

import java.time.LocalDateTime;

/**
 * Kafka로 발행될 도메인 이벤트의 기본 인터페이스
 */
public interface DomainEvent {

    /**
     * 이벤트 ID (멱등성 보장용)
     */
    String getEventId();

    /**
     * 이벤트 타입
     */
    String getEventType();

    /**
     * Aggregate 타입 (Product, Order 등)
     */
    String getAggregateType();

    /**
     * Aggregate ID (파티션 키로 사용)
     */
    String getAggregateId();

    /**
     * 이벤트 발생 시각
     */
    LocalDateTime getOccurredAt();

    /**
     * Kafka 토픽 이름
     */
    String getTopic();
}
