package com.pentakill.ssok.payment.domain;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);

    // 트랜잭션이 끝날 때까지 다른 요청이 같은 결제를 바꾸지 못하게 잠그고 읽는다.
    Optional<Payment> findByIdForUpdate(Long id);

    Optional<Payment> findByOrderId(Long orderId);
}
