package com.pentakill.ssok.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class PaymentServiceTest {

    private static final long ORDER_ID = 1L;
    private static final long MEMBER_ID = 10L;
    private static final long OTHER_MEMBER_ID = 20L;
    private static final long AMOUNT = 30_000L;
    private static final String PAYMENT_KEY = "pay_test_key";
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 10, 27, 14, 0);
    private static final LocalDateTime CANCELED_AT = LocalDateTime.of(2026, 10, 28, 10, 0);

    private FakePaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
    }

    @Nested
    @DisplayName("결제 준비")
    class Prepare {

        @Test
        @DisplayName("처음 준비하면 주문 금액으로 READY 결제를 만들어 저장한다")
        void createsNewPayment() {
            PaymentService service = serviceWith(payableOrder());

            Payment payment = service.prepare(ORDER_ID, MEMBER_ID);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(payment.getAmount()).isEqualTo(AMOUNT);
            assertThat(paymentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("주문이 없으면 404 예외가 난다")
        void rejectsMissingOrder() {
            PaymentService service = serviceWith(null);

            assertThatThrownBy(() -> service.prepare(ORDER_ID, MEMBER_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("남의 주문이면 결제가 이미 있어도 404 예외가 난다")
        void rejectsOtherMembersOrder() {
            PaymentService service = serviceWith(payableOrder());
            savedPaymentWithStatus(PaymentStatus.READY);

            assertThatThrownBy(() -> service.prepare(ORDER_ID, OTHER_MEMBER_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("결제할 수 없는 주문이면 거절한다")
        void rejectsNotPayableOrder() {
            PaymentService service = serviceWith(new OrderInfo(ORDER_ID, MEMBER_ID, AMOUNT, false));

            assertThatThrownBy(() -> service.prepare(ORDER_ID, MEMBER_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(paymentRepository.count()).isZero();
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "CANCELED"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("끝나지 않은 결제가 있으면 새로 만들지 않고 그 결제를 돌려준다")
        void reusesUnfinishedPayment(PaymentStatus status) {
            PaymentService service = serviceWith(payableOrder());
            Payment existing = savedPaymentWithStatus(status);

            Payment payment = service.prepare(ORDER_ID, MEMBER_ID);

            assertThat(payment).isSameAs(existing);
            assertThat(paymentRepository.count()).isEqualTo(1);
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "CANCELED"})
        @DisplayName("이미 승인되거나 취소된 결제가 있으면 거절한다")
        void rejectsFinishedPayment(PaymentStatus status) {
            PaymentService service = serviceWith(payableOrder());
            savedPaymentWithStatus(status);

            assertThatThrownBy(() -> service.prepare(ORDER_ID, MEMBER_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("승인 시작")
    class StartApproval {

        @Test
        @DisplayName("내 결제면 IN_PROGRESS로 바뀌고 결제 키가 저장된다")
        void startsApproval() {
            PaymentService service = serviceWith(payableOrder());
            Payment saved = savedPaymentWithStatus(PaymentStatus.READY);

            Payment payment = service.startApproval(saved.getId(), MEMBER_ID, PAYMENT_KEY, AMOUNT);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
            assertThat(payment.getPaymentKey()).isEqualTo(PAYMENT_KEY);
        }

        @Test
        @DisplayName("없는 결제면 404 예외가 난다")
        void rejectsMissingPayment() {
            PaymentService service = serviceWith(payableOrder());

            assertThatThrownBy(() -> service.startApproval(999L, MEMBER_ID, PAYMENT_KEY, AMOUNT))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("남의 결제면 404 예외가 난다")
        void rejectsOtherMembersPayment() {
            PaymentService service = serviceWith(payableOrder());
            Payment saved = savedPaymentWithStatus(PaymentStatus.READY);

            assertThatThrownBy(() -> service.startApproval(saved.getId(), OTHER_MEMBER_ID, PAYMENT_KEY, AMOUNT))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("금액이 다르면 승인을 시작하지 않는다")
        void rejectsDifferentAmount() {
            PaymentService service = serviceWith(payableOrder());
            Payment saved = savedPaymentWithStatus(PaymentStatus.READY);

            assertThatThrownBy(() -> service.startApproval(saved.getId(), MEMBER_ID, PAYMENT_KEY, AMOUNT - 1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.READY);
        }
    }

    @Nested
    @DisplayName("승인 결과 저장")
    class CompleteApproval {

        @ParameterizedTest
        @CsvSource({
                "APPROVED, APPROVED",
                "REJECTED, FAILED",
                "UNKNOWN, UNKNOWN"
        })
        @DisplayName("PG의 대답에 맞는 상태로 바뀐다")
        void changesStatusByResult(PgApprovalResult result, PaymentStatus expected) {
            PaymentService service = serviceWith(payableOrder());
            Payment saved = savedPaymentWithStatus(PaymentStatus.IN_PROGRESS);

            Payment payment = service.completeApproval(saved.getId(), result, APPROVED_AT);

            assertThat(payment.getStatus()).isEqualTo(expected);
        }

        @Test
        @DisplayName("승인되면 승인 시각이 저장된다")
        void savesApprovedAt() {
            PaymentService service = serviceWith(payableOrder());
            Payment saved = savedPaymentWithStatus(PaymentStatus.IN_PROGRESS);

            Payment payment = service.completeApproval(saved.getId(), PgApprovalResult.APPROVED, APPROVED_AT);

            assertThat(payment.getApprovedAt()).isEqualTo(APPROVED_AT);
        }
    }

    private PaymentService serviceWith(OrderInfo order) {
        OrderReader orderReader = orderId -> Optional.ofNullable(order);
        return new PaymentService(orderReader, paymentRepository);
    }

    private OrderInfo payableOrder() {
        return new OrderInfo(ORDER_ID, MEMBER_ID, AMOUNT, true);
    }

    private Payment savedPaymentWithStatus(PaymentStatus status) {
        Payment payment = Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT);
        if (status != PaymentStatus.READY) {
            payment.startApproval(PAYMENT_KEY, AMOUNT);
        }
        switch (status) {
            case APPROVED -> payment.approve(APPROVED_AT);
            case FAILED -> payment.fail();
            case UNKNOWN -> payment.markUnknown();
            case CANCELED -> {
                payment.approve(APPROVED_AT);
                payment.cancel("고객 요청", CANCELED_AT);
            }
            default -> {
            }
        }
        return paymentRepository.save(payment);
    }
}
