package com.pentakill.ssok.payment.application;

import java.util.Optional;

public interface OrderReader {
    Optional<OrderInfo> findOrder(Long orderId);
}
