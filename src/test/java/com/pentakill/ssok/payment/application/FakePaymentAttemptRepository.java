package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.PaymentAttempt;
import com.pentakill.ssok.payment.domain.PaymentAttemptRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class FakePaymentAttemptRepository implements PaymentAttemptRepository {

    private final List<PaymentAttempt> attempts = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        if (attempt.getId() == null) {
            ReflectionTestUtils.setField(attempt, "id", nextId++);
            attempts.add(attempt);
        }
        return attempt;
    }

    @Override
    public Optional<PaymentAttempt> findById(Long id) {
        return attempts.stream()
                .filter(attempt -> id.equals(attempt.getId()))
                .findFirst();
    }

    @Override
    public Optional<PaymentAttempt> findByIdempotencyKey(String idempotencyKey) {
        return attempts.stream()
                .filter(attempt -> idempotencyKey.equals(attempt.getIdempotencyKey()))
                .findFirst();
    }

    List<PaymentAttempt> findAll() {
        return List.copyOf(attempts);
    }
}
