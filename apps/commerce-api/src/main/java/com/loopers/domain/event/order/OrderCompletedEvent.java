package com.loopers.domain.event.order;

import com.loopers.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 주문 완료 이벤트 (Kafka 발행용)
 * - Topic: order-events
 * - PartitionKey: orderId
 */
public record OrderCompletedEvent(
    String eventId,
    String eventType,
    Long orderId,
    String userId,
    List<OrderItemDto> orderItems,
    BigDecimal totalAmount,
    BigDecimal finalAmount,
    LocalDateTime occurredAt
) implements DomainEvent {

    private static final String TOPIC = "order-events";
    private static final String AGGREGATE_TYPE = "Order";
    private static final String EVENT_TYPE = "OrderCompleted";

    /**
     * 주문 완료 이벤트 생성
     */
    public static OrderCompletedEvent of(
        Long orderId,
        String userId,
        List<OrderItemDto> orderItems,
        BigDecimal totalAmount,
        BigDecimal finalAmount
    ) {
        return new OrderCompletedEvent(
            UUID.randomUUID().toString(),
            EVENT_TYPE,
            orderId,
            userId,
            orderItems,
            totalAmount,
            finalAmount,
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
        return orderId.toString();
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getTopic() {
        return TOPIC;
    }

    /**
     * 주문 항목 DTO
     */
    public record OrderItemDto(
        Long productId,
        String productName,
        BigDecimal price,
        int quantity
    ) {}
}
