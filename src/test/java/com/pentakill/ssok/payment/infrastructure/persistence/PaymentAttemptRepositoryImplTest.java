package com.pentakill.ssok.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentAttempt;
import com.pentakill.ssok.payment.domain.PaymentAttemptRepository;
import com.pentakill.ssok.payment.domain.PaymentAttemptStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PaymentAttemptRepositoryImpl.class)
class PaymentAttemptRepositoryImplTest {

    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 11, 15, 14, 0);

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = entityManager.persist(Payment.ready(101L, 1L, 30_000L));
    }

    @Test
    @DisplayName("멱등키로 시도를 찾을 수 있고, 없는 키면 빈 결과가 나온다")
    void findsByIdempotencyKey() {
        paymentAttemptRepository.save(PaymentAttempt.start(payment, "idem-1", "pay_key", REQUESTED_AT));
        entityManager.flush();
        entityManager.clear();

        PaymentAttempt found = paymentAttemptRepository.findByIdempotencyKey("idem-1").orElseThrow();

        assertThat(found.getStatus()).isEqualTo(PaymentAttemptStatus.IN_PROGRESS);
        assertThat(found.isFor(payment.getId())).isTrue();
        assertThat(paymentAttemptRepository.findByIdempotencyKey("idem-none")).isEmpty();
    }

    @Test
    @DisplayName("같은 멱등키로 시도를 두 번 저장하면 유니크 제약이 막는다")
    void rejectsDuplicateIdempotencyKey() {
        paymentAttemptRepository.save(PaymentAttempt.start(payment, "idem-1", "pay_key", REQUESTED_AT));
        entityManager.flush();

        assertThatThrownBy(() -> {
            paymentAttemptRepository.save(PaymentAttempt.start(payment, "idem-1", "pay_key", REQUESTED_AT));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
