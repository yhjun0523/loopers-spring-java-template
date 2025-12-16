package com.loopers.domain.event.catalog;

import com.loopers.domain.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상품 좋아요 토글 이벤트 (Kafka 발행용)
 * - Topic: catalog-events
 * - PartitionKey: productId
 */
public record ProductLikeToggledEvent(
    String eventId,
    String eventType,
    Long productId,
    String userId,
    boolean isAdded,
    LocalDateTime occurredAt
) implements DomainEvent {

    private static final String TOPIC = "catalog-events";
    private static final String AGGREGATE_TYPE = "Product";
    private static final String EVENT_TYPE_LIKE_ADDED = "ProductLikeAdded";
    private static final String EVENT_TYPE_LIKE_REMOVED = "ProductLikeRemoved";

    /**
     * 좋아요 추가 이벤트 생성
     */
    public static ProductLikeToggledEvent added(Long productId, String userId) {
        return new ProductLikeToggledEvent(
            UUID.randomUUID().toString(),
            EVENT_TYPE_LIKE_ADDED,
            productId,
            userId,
            true,
            LocalDateTime.now()
        );
    }

    /**
     * 좋아요 제거 이벤트 생성
     */
    public static ProductLikeToggledEvent removed(Long productId, String userId) {
        return new ProductLikeToggledEvent(
            UUID.randomUUID().toString(),
            EVENT_TYPE_LIKE_REMOVED,
            productId,
            userId,
            false,
            LocalDateTime.now()
        );
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getAggregateType() {
        return AGGREGATE_TYPE;
    }

    @Override
    public String getAggregateId() {
        return productId.toString();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }
}
