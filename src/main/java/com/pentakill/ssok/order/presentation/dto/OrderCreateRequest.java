package com.pentakill.ssok.order.presentation.dto;

public record OrderCreateRequest (
    Long memberId,
    Long deliveryId,
    Long orderItemId,
    long shippingFee,
    long totalAmount){

}
