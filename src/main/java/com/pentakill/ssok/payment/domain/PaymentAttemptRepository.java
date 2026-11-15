package com.pentakill.ssok.payment.domain;

import java.util.Optional;

public interface PaymentAttemptRepository {
    PaymentAttempt save(PaymentAttempt attempt);
    Optional<PaymentAttempt> findById(Long id);
    Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey);
}
