package com.pentakill.ssok.payment.domain;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByIdForUpdate(Long id);
    Optional<Payment> findByOrderIdForUpdate(Long orderId);
}
