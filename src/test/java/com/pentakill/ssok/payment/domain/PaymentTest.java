package com.pentakill.ssok.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentTest {

    private static final long ORDER_ID = 1L;
    private static final long MEMBER_ID = 10L;
    private static final long AMOUNT = 30_000L;
    private static final String PAYMENT_KEY = "pay_test_key";
    private static final LocalDateTime APPROVED_AT = LocalDateTime.of(2026, 10, 12, 14, 0);
    private static final LocalDateTime CANCELED_AT = LocalDateTime.of(2026, 10, 13, 10, 0);

    @Nested
    @DisplayName("결제 준비")
    class Ready {

        @Test
        @DisplayName("결제를 준비하면 READY 상태로 만들어진다")
        void createsReadyPayment() {
            Payment payment = Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(payment.getAmount()).isEqualTo(AMOUNT);
        }

        @Test
        @DisplayName("금액이 0원 이하면 결제를 준비할 수 없다")
        void rejectsNonPositiveAmount() {
            assertThatThrownBy(() -> Payment.ready(ORDER_ID, MEMBER_ID, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("승인 시작")
    class StartApproval {

        @Test
        @DisplayName("READY인 결제는 승인을 시작할 수 있다")
        void startsFromReady() {
            Payment payment = paymentWithStatus(PaymentStatus.READY);

            payment.startApproval(PAYMENT_KEY, AMOUNT);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
            assertThat(payment.getPaymentKey()).isEqualTo(PAYMENT_KEY);
        }

        @Test
        @DisplayName("FAILED인 결제는 승인을 다시 시작할 수 있다")
        void restartsFromFailed() {
            Payment payment = paymentWithStatus(PaymentStatus.FAILED);

            payment.startApproval("pay_retry_key", AMOUNT);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
            assertThat(payment.getPaymentKey()).isEqualTo("pay_retry_key");
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = {"READY", "FAILED"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("READY, FAILED가 아니면 승인을 시작할 수 없다")
        void rejectsOtherStatuses(PaymentStatus status) {
            Payment payment = paymentWithStatus(status);

            assertThatThrownBy(() -> payment.startApproval(PAYMENT_KEY, AMOUNT))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(payment.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("요청 금액이 저장된 금액과 다르면 승인을 시작할 수 없고, 아무 값도 바뀌지 않는다")
        void rejectsDifferentAmount() {
            Payment payment = paymentWithStatus(PaymentStatus.READY);

            assertThatThrownBy(() -> payment.startApproval(PAYMENT_KEY, 100L))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
            assertThat(payment.getPaymentKey()).isNull();
        }
    }

    @Nested
    @DisplayName("승인 완료")
    class Approve {

        @Test
        @DisplayName("IN_PROGRESS인 결제를 승인하면 APPROVED가 되고 승인 시각이 기록된다")
        void approvesInProgress() {
            Payment payment = paymentWithStatus(PaymentStatus.IN_PROGRESS);

            payment.approve(APPROVED_AT);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getApprovedAt()).isEqualTo(APPROVED_AT);
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = "IN_PROGRESS", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("IN_PROGRESS가 아니면 승인할 수 없고, 상태가 그대로 남는다")
        void rejectsOtherStatuses(PaymentStatus status) {
            Payment payment = paymentWithStatus(status);

            assertThatThrownBy(() -> payment.approve(APPROVED_AT))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(payment.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("승인 시각이 없으면 승인할 수 없다")
        void rejectsNullApprovedAt() {
            Payment payment = paymentWithStatus(PaymentStatus.IN_PROGRESS);

            assertThatThrownBy(() -> payment.approve(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("승인 실패와 결과 모름")
    class FailAndUnknown {

        @Test
        @DisplayName("IN_PROGRESS인 결제가 거절되면 FAILED가 된다")
        void failsInProgress() {
            Payment payment = paymentWithStatus(PaymentStatus.IN_PROGRESS);

            payment.fail();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("IN_PROGRESS인 결제의 응답이 없으면 UNKNOWN이 된다")
        void marksUnknownInProgress() {
            Payment payment = paymentWithStatus(PaymentStatus.IN_PROGRESS);

            payment.markUnknown();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = "IN_PROGRESS", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("IN_PROGRESS가 아니면 실패나 결과 모름으로 바꿀 수 없다")
        void rejectsOtherStatuses(PaymentStatus status) {
            Payment payment = paymentWithStatus(status);

            assertThatThrownBy(payment::fail).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(payment::markUnknown).isInstanceOf(IllegalStateException.class);
            assertThat(payment.getStatus()).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("결제 취소")
    class Cancel {

        @Test
        @DisplayName("APPROVED인 결제를 취소하면 CANCELED가 되고 사유와 시각이 기록된다")
        void cancelsApproved() {
            Payment payment = paymentWithStatus(PaymentStatus.APPROVED);

            payment.cancel("단순 변심", CANCELED_AT);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
            assertThat(payment.getCancelReason()).isEqualTo("단순 변심");
            assertThat(payment.getCanceledAt()).isEqualTo(CANCELED_AT);
        }

        @ParameterizedTest
        @EnumSource(value = PaymentStatus.class, names = "APPROVED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("APPROVED가 아니면 취소할 수 없고, 상태가 그대로 남는다")
        void rejectsOtherStatuses(PaymentStatus status) {
            Payment payment = paymentWithStatus(status);

            assertThatThrownBy(() -> payment.cancel("단순 변심", CANCELED_AT))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(payment.getStatus()).isEqualTo(status);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("취소 사유가 없거나 공백이면 취소할 수 없다")
        void rejectsBlankReason(String cancelReason) {
            Payment payment = paymentWithStatus(PaymentStatus.APPROVED);

            assertThatThrownBy(() -> payment.cancel(cancelReason, CANCELED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getCancelReason()).isNull();
        }

        @Test
        @DisplayName("취소 시각이 없으면 취소할 수 없다")
        void rejectsNullCanceledAt() {
            Payment payment = paymentWithStatus(PaymentStatus.APPROVED);

            assertThatThrownBy(() -> payment.cancel("단순 변심", null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        }
    }

    private Payment paymentWithStatus(PaymentStatus status) {
        Payment payment = Payment.ready(ORDER_ID, MEMBER_ID, AMOUNT);
        if (status == PaymentStatus.READY) {
            return payment;
        }
        payment.startApproval(PAYMENT_KEY, AMOUNT);
        switch (status) {
            case IN_PROGRESS -> { }
            case APPROVED -> payment.approve(APPROVED_AT);
            case FAILED -> payment.fail();
            case UNKNOWN -> payment.markUnknown();
            case CANCELED -> {
                payment.approve(APPROVED_AT);
                payment.cancel("테스트 취소", CANCELED_AT);
            }
        }
        return payment;
    }
}