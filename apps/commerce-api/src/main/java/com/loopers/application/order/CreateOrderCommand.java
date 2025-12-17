package com.loopers.application.order;

import com.loopers.domain.order.OrderService;
import lombok.Getter;

import java.util.List;

/**
 * 주문 생성 커맨드
 */
@Getter
public class CreateOrderCommand {
    private final String userId;
    private final List<OrderService.OrderItemRequest> orderItems;
    private final int usedPoints;
    private final Long couponId; // nullable

    public CreateOrderCommand(
            String userId,
            List<OrderService.OrderItemRequest> orderItems,
            int usedPoints,
            Long couponId
    ) {
        this.userId = userId;
        this.orderItems = orderItems;
        this.usedPoints = usedPoints;
        this.couponId = couponId;
    }
}
