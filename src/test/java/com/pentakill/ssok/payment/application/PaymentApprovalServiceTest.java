package com.pentakill.ssok.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pentakill.ssok.payment.domain.Payment;
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

    private FakePaymentRepository paymentRepository;
    private AtomicInteger pgCalls;

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
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

        Payment payment = serviceWith(pgResult).approve(saved.getId(), MEMBER_ID, PAYMENT_KEY, AMOUNT);

        assertThat(payment.getStatus()).isEqualTo(expected);
        assertThat(pgCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("금액이 다르면 PG를 부르지 않는다")
    void doesNotCallPgWhenAmountDiffers() {
        Payment saved = paymentRepository.save(Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT));

        assertThatThrownBy(() -> serviceWith(PgApprovalResult.APPROVED)
                .approve(saved.getId(), MEMBER_ID, PAYMENT_KEY, AMOUNT - 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(pgCalls.get()).isZero();
    }

    private PaymentApprovalService serviceWith(PgApprovalResult pgResult) {
        PaymentService paymentService = new PaymentService(orderId -> Optional.empty(), paymentRepository);
        PaymentGateway paymentGateway = (paymentKey, orderId, amount) -> {
            pgCalls.incrementAndGet();
            return pgResult;
        };
        return new PaymentApprovalService(paymentService, paymentGateway);
    }
}
