package com.pentakill.ssok.order.presentation.dto;

import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderItem;
import com.pentakill.ssok.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(Long orderId, Long memberId, Long deliveryId,
                                  OrderStatus status, LocalDateTime orderAt,
                                  long shippingFee, long totalAmount,
                                  List<OrderItemResponse> items) {
    public static OrderDetailResponse from(Order order, List<OrderItem> items) {
        return new OrderDetailResponse(order.getId(), order.getMemberId(), order.getDeliveryId(),
                order.getStatus(), order.getOrderAt(), order.getShippingFee(), order.getTotalAmount(),
                items.stream().map(OrderItemResponse::from).toList());
    }
}
