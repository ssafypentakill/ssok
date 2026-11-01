package com.pentakill.ssok.order.presentation.dto;

import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderDetailResponse (Long orderId, Long memberId, Long deliveryId, Long orderItemId, OrderStatus status, LocalDateTime orderAt, long shippingFee, long totalAmount) {

    public static OrderDetailResponse from(Order order) {return new OrderDetailResponse(
            order.getId(),
            order.getMemberId(),
            order.getDeliveryId(),
            order.getOrderItemId(),
            order.getStatus(),
            order.getOrderAt(),
            order.getShippingFee(),
            order.getTotalAmount()

            );
    }
}
