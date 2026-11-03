package com.pentakill.ssok.cart.presentation;

import java.util.List;

public class CartDto {

    public record AddRequest(
            Long productId,
            Integer quantity
    ) {}

    // 수량 변경 요청
    public record UpdateQuantityRequest(
            Integer quantity
    ) {}

    // 장바구니 조회 응답
    public record Response(
            Long cartId,
            List<ItemResponse> items,
            long totalPrice
    ) {}

    // 장바구니 안의 상품 하나
    public record ItemResponse(
            Long cartItemId,
            Long productId,
            String productName,
            long unitPrice,
            int quantity,
            long subtotal
    ) {}

}
