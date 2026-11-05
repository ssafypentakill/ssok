package com.pentakill.ssok.order.presentation.dto;

public record DirectOrderRequest (
    Long deliveryId,
    Long productId,
    int quantity){

}
