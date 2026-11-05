package com.pentakill.ssok.order.presentation.dto;

import com.pentakill.ssok.order.domain.OrderItem;

public record OrderItemResponse(Long orderItemId, Long productId, String productName,
                                int quantity, long unitPrice, long subtotal) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProductId(), item.getProductName(),
                item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
    }
}
