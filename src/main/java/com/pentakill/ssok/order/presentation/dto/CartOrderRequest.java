package com.pentakill.ssok.order.presentation.dto;

import java.util.List;

public record CartOrderRequest (
    Long deliveryId,
    List<Long> cartItemIds){
}
