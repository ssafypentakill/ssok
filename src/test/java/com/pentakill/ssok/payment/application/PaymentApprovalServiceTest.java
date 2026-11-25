package com.pentakill.ssok.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentAttempt;
import com.pentakill.ssok.payment.domain.PaymentAttemptStatus;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaymentApprovalServiceTest {

    private static final long ORDER_ID = 1L;
    private static final long MEMBER_ID = 10L;
    private static final long AMOUNT = 30_000L;
    private static final String PAYMENT_KEY = "pay_test_key";

    private static final String IDEMPOTENCY_KEY = "idem-1";

    private FakePaymentRepository paymentRepository;
    private FakePaymentAttemptRepository paymentAttemptRepository;
    private AtomicInteger pgCalls;

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
        paymentAttemptRepository = new FakePaymentAttemptRepository();
        pgCalls = new AtomicInteger();
    }

    @ParameterizedTest
    @CsvSource({
            "APPROVED, APPROVED",
            "REJECTED, FAILED",
            "UNKNOWN, UNKNOWN"
    })
    @DisplayName("승인을 시작하고 PG의 대답에 맞는 상태로 끝난다")
    void approvesThroughPg(PgApprovalResult pgResult, PaymentStatus expected) {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));

        Payment payment = serviceWith(pgResult).approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT);

        assertThat(payment.getStatus()).isEqualTo(expected);
        assertThat(pgCalls.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            "APPROVED, APPROVED",
            "REJECTED, FAILED",
            "UNKNOWN, UNKNOWN"
    })
    @DisplayName("승인 요청마다 시도 기록이 남고, PG의 대답이 시도에도 저장된다")
    void recordsAttempt(PgApprovalResult pgResult, PaymentAttemptStatus expected) {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));

        serviceWith(pgResult).approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT);

        assertThat(paymentAttemptRepository.findAll()).singleElement().satisfies(attempt -> {
            assertThat(attempt.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
            assertThat(attempt.getStatus()).isEqualTo(expected);
            assertThat(attempt.getRequestedAt()).isNotNull();
            assertThat(attempt.getCompletedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("거절된 뒤 새 멱등키로 다시 승인하면 시도 기록이 하나 더 남는다")
    void recordsEveryRetry() {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));
        serviceWith(PgApprovalResult.REJECTED).approve(saved.getId(), MEMBER_ID, "idem-1", PAYMENT_KEY, AMOUNT);

        Payment payment = serviceWith(PgApprovalResult.APPROVED)
                .approve(saved.getId(), MEMBER_ID, "idem-2", PAYMENT_KEY, AMOUNT);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(paymentAttemptRepository.findAll())
                .extracting(PaymentAttempt::getStatus)
                .containsExactly(PaymentAttemptStatus.FAILED, PaymentAttemptStatus.APPROVED);
    }

    @Test
    @DisplayName("같은 멱등키로 다시 승인하면 PG를 다시 부르지 않고 지금 결제 상태를 돌려준다")
    void replaysSameIdempotencyKey() {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));
        PaymentApprovalService service = serviceWith(PgApprovalResult.APPROVED);
        service.approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT);

        Payment replayed = service.approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT);

        assertThat(replayed.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(pgCalls.get()).isEqualTo(1);
        assertThat(paymentAttemptRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("같은 멱등키라도 금액이 다르면 거절한다")
    void rejectsDifferentAmountEvenWithSameKey() {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));
        PaymentApprovalService service = serviceWith(PgApprovalResult.APPROVED);
        service.approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT);

        assertThatThrownBy(() -> service.approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT - 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(pgCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 결제에 쓰인 멱등키로 승인하면 PG를 부르지 않고 거절한다")
    void rejectsKeyUsedByOtherPayment() {
        Payment first = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));
        Payment second = paymentRepository.save(Payment.ready(ORDER_ID + 1, MEMBER_ID, AMOUNT));
        PaymentApprovalService service = serviceWith(PgApprovalResult.APPROVED);
        service.approve(first.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT);

        assertThatThrownBy(() -> service.approve(second.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT))
                .isInstanceOf(IllegalStateException.class);
        assertThat(second.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(pgCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("멱등키가 없으면 결제를 바꾸지 않고 PG도 부르지 않는다")
    void rejectsMissingIdempotencyKey() {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));

        assertThatThrownBy(() -> serviceWith(PgApprovalResult.APPROVED)
                .approve(saved.getId(), MEMBER_ID, " ", PAYMENT_KEY, AMOUNT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(pgCalls.get()).isZero();
    }

    @Test
    @DisplayName("금액이 다르면 PG를 부르지 않는다")
    void doesNotCallPgWhenAmountDiffers() {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));

        assertThatThrownBy(() -> serviceWith(PgApprovalResult.APPROVED)
                .approve(saved.getId(), MEMBER_ID, IDEMPOTENCY_KEY, PAYMENT_KEY, AMOUNT - 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(pgCalls.get()).isZero();
    }

    private PaymentApprovalService serviceWith(PgApprovalResult pgResult) {
        PaymentService paymentService = new PaymentService(orderId -> Optional.empty(), paymentRepository, paymentAttemptRepository);
        PaymentGateway paymentGateway = new PaymentGateway() {
            @Override
            public PgApprovalResult approve(String paymentKey, Long orderId, long amount) {
                pgCalls.incrementAndGet();
                return pgResult;
            }

            @Override
            public void cancel(String paymentKey, String cancelReason) {
            }
        };
        return new PaymentApprovalService(paymentService, paymentGateway);
    }
}
