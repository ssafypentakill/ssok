package com.pentakill.ssok.payment.infrastructure.persistence;

import com.pentakill.ssok.payment.domain.PaymentAttempt;
import com.pentakill.ssok.payment.domain.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentAttemptRepositoryImpl implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository paymentAttemptJpaRepository;

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        return paymentAttemptJpaRepository.save(attempt);
    }

    @Override
    public Optional<PaymentAttempt> findById(Long id) {
        return paymentAttemptJpaRepository.findById(id);
    }

    @Override
    public Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey) {
        return paymentAttemptJpaRepository.findByIdempotencyKey(idempotencyKey);
    }
}
