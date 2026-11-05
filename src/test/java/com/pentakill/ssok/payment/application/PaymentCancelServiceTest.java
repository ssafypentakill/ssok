package com.pentakill.ssok.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentCancelServiceTest {

    private static final long ORDER_ID = 1L;
    private static final long MEMBER_ID = 10L;
    private static final long OTHER_MEMBER_ID = 20L;
    private static final long AMOUNT = 30_000L;
    private static final String PAYMENT_KEY = "pay_test_key";
    private static final String CANCEL_REASON = "고객 요청";
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 11, 1, 14, 0);

    private FakePaymentRepository paymentRepository;
    private AtomicInteger pgCancelCalls;
    private PaymentCancelService service;
    private OrderPaymentUpdater orderUpdater;

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
        pgCancelCalls = new AtomicInteger();
        PaymentService paymentService = new PaymentService(orderId -> Optional.empty(), paymentRepository, org.mockito.Mockito.mock(OrderPaymentUpdater.class));
        PaymentGateway paymentGateway = new PaymentGateway() {
            @Override
            public PgApprovalResult approve(String paymentKey, Long orderId, long amount) {
                return PgApprovalResult.APPROVED;
            }

            @Override
            public void cancel(String paymentKey, String cancelReason) {
                pgCancelCalls.incrementAndGet();
            }
        };
        orderUpdater = org.mockito.Mockito.mock(OrderPaymentUpdater.class);
        service = new PaymentCancelService(paymentService, paymentGateway, orderUpdater);
    }

    @Test
    @DisplayName("승인된 내 결제는 PG에 취소를 요청하고 CANCELED가 된다")
    void cancelsApprovedPayment() {
        Payment saved = savedPayment(PaymentStatus.APPROVED);

        Payment payment = service.cancel(saved.getId(), MEMBER_ID, CANCEL_REASON);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getCancelReason()).isEqualTo(CANCEL_REASON);
        assertThat(pgCancelCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 ID로 취소하면 주문 상태와 재고 복구를 요청한다")
    void cancelsByOrderIdAndRestoresOrder() {
        Payment saved = savedPayment(PaymentStatus.APPROVED);

        Payment canceled = service.cancelByOrderId(ORDER_ID, MEMBER_ID, CANCEL_REASON);

        assertThat(canceled).isSameAs(saved);
        assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(pgCancelCalls.get()).isEqualTo(1);
        org.mockito.Mockito.verify(orderUpdater).requirePaidOrder(ORDER_ID, MEMBER_ID);
        org.mockito.Mockito.verify(orderUpdater).cancelAndRestore(ORDER_ID, MEMBER_ID);
    }

    @Test
    @DisplayName("같은 주문을 다시 취소하면 PG를 재호출하지 않는다")
    void rejectsRepeatedOrderCancellation() {
        savedPayment(PaymentStatus.APPROVED);
        service.cancelByOrderId(ORDER_ID, MEMBER_ID, CANCEL_REASON);

        assertThatThrownBy(() -> service.cancelByOrderId(ORDER_ID, MEMBER_ID, CANCEL_REASON))
                .isInstanceOf(IllegalStateException.class);
        assertThat(pgCancelCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("남의 결제면 404 예외가 나고 PG를 부르지 않는다")
    void rejectsOtherMembersPayment() {
        Payment saved = savedPayment(PaymentStatus.APPROVED);

        assertThatThrownBy(() -> service.cancel(saved.getId(), OTHER_MEMBER_ID, CANCEL_REASON))
                .isInstanceOf(EntityNotFoundException.class);
        assertThat(pgCancelCalls.get()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"READY", "IN_PROGRESS", "FAILED", "UNKNOWN"})
    @DisplayName("승인되지 않은 결제는 PG를 부르지 않고 거절한다")
    void rejectsNotApprovedPayment(PaymentStatus status) {
        Payment saved = savedPayment(status);

        assertThatThrownBy(() -> service.cancel(saved.getId(), MEMBER_ID, CANCEL_REASON))
                .isInstanceOf(IllegalStateException.class);
        assertThat(pgCancelCalls.get()).isZero();
    }

    private Payment savedPayment(PaymentStatus status) {
        Payment payment = Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT);
        if (status != PaymentStatus.READY) {
            payment.startApproval(PAYMENT_KEY, AMOUNT);
        }
        switch (status) {
            case APPROVED -> payment.approve(APPROVED_AT);
            case FAILED -> payment.fail();
            case UNKNOWN -> payment.markUnknown();
            default -> {
            }
        }
        return paymentRepository.save(payment);
    }
}
