package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주문 응답 DTO
 */
@Getter
public class OrderResponse {
    private final Long orderId;
    private final String userId;
    private final BigDecimal totalAmount;
    private final BigDecimal couponDiscount;
    private final int usedPoints;
    private final BigDecimal finalAmount;
    private final Long couponId;
    private final OrderStatus status;
    private final LocalDateTime orderedAt;
    private final LocalDateTime modifiedAt;
    private final List<OrderItemResponse> orderItems;

    private OrderResponse(Long orderId, String userId, BigDecimal totalAmount, BigDecimal couponDiscount,
                          int usedPoints, BigDecimal finalAmount, Long couponId, OrderStatus status,
                          LocalDateTime orderedAt, LocalDateTime modifiedAt, List<OrderItemResponse> orderItems) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.couponDiscount = couponDiscount;
        this.usedPoints = usedPoints;
        this.finalAmount = finalAmount;
        this.couponId = couponId;
        this.status = status;
        this.orderedAt = orderedAt;
        this.modifiedAt = modifiedAt;
        this.orderItems = orderItems;
    }

    /**
     * Order 도메인 객체로부터 DTO 생성
     */
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCouponDiscount(),
                order.getUsedPoints(),
                order.getFinalAmount(),
                order.getCouponId(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getModifiedAt(),
                itemResponses
        );
    }

    /**
     * 주문 항목 응답 DTO
     */
    @Getter
    public static class OrderItemResponse {
        private final Long orderItemId;
        private final Long productId;
        private final String productName;
        private final BigDecimal price;
        private final int quantity;
        private final BigDecimal subtotal;

        private OrderItemResponse(Long orderItemId, Long productId, String productName,
                                  BigDecimal price, int quantity, BigDecimal subtotal) {
            this.orderItemId = orderItemId;
            this.productId = productId;
            this.productName = productName;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public static OrderItemResponse from(OrderItem orderItem) {
            return new OrderItemResponse(
                    orderItem.getId(),
                    orderItem.getProductId(),
                    orderItem.getProductName(),
                    orderItem.getPrice(),
                    orderItem.getQuantity(),
                    orderItem.getSubtotal()
            );
        }
    }
}
