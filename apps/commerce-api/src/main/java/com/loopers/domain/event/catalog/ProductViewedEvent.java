package com.loopers.domain.event.catalog;

import com.loopers.domain.event.DomainEvent;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 상품 조회 이벤트 (Kafka 발행용)
 * - Topic: catalog-events
 * - PartitionKey: productId
 */
public record ProductViewedEvent(
    String eventId,
    String eventType,
    Long productId,
    String userId,
    ZonedDateTime occurredAt
) implements DomainEvent {

    private static final String TOPIC = "catalog-events";
    private static final String AGGREGATE_TYPE = "Product";
    private static final String EVENT_TYPE = "ProductViewed";

    /**
     * 상품 조회 이벤트 생성
     */
    public static ProductViewedEvent of(Long productId, String userId) {
        return new ProductViewedEvent(
            UUID.randomUUID().toString(),
            EVENT_TYPE,
            productId,
            userId,
            ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
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
    public ZonedDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }
}
