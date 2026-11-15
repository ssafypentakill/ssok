package com.pentakill.ssok.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PaymentAttemptTest {

    private static final String IDEMPOTENCY_KEY = "idem-1";
    private static final String PAYMENT_KEY = "pay_test_key";
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 11, 15, 14, 0, 0);
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 11, 15, 14, 0, 3);

    private final Payment payment = Payment.ready(1L, 10L, 30_000L);

    @Nested
    @DisplayName("시도 시작")
    class Start {

        @Test
        @DisplayName("시도를 시작하면 IN_PROGRESS로 만들어지고 요청 시각이 기록된다")
        void startsInProgress() {
            PaymentAttempt attempt = PaymentAttempt.start(payment, IDEMPOTENCY_KEY, PAYMENT_KEY, REQUESTED_AT);

            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.IN_PROGRESS);
            assertThat(attempt.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
            assertThat(attempt.getPaymentKey()).isEqualTo(PAYMENT_KEY);
            assertThat(attempt.getRequestedAt()).isEqualTo(REQUESTED_AT);
            assertThat(attempt.getCompletedAt()).isNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("멱등키가 없거나 공백이면 시도를 만들 수 없다")
        void rejectsBlankIdempotencyKey(String idempotencyKey) {
            assertThatThrownBy(() -> PaymentAttempt.start(payment, idempotencyKey, PAYMENT_KEY, REQUESTED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("멱등키가 100자를 넘으면 시도를 만들 수 없다")
        void rejectsTooLongIdempotencyKey() {
            String tooLong = "a".repeat(101);

            assertThatThrownBy(() -> PaymentAttempt.start(payment, tooLong, PAYMENT_KEY, REQUESTED_AT))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("요청 시각이 없으면 시도를 만들 수 없다")
        void rejectsNullRequestedAt() {
            assertThatThrownBy(() -> PaymentAttempt.start(payment, IDEMPOTENCY_KEY, PAYMENT_KEY, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("시도 결과")
    class Complete {

        @Test
        @DisplayName("승인되면 APPROVED가 되고 응답 시각이 기록된다")
        void approves() {
            PaymentAttempt attempt = PaymentAttempt.start(payment, IDEMPOTENCY_KEY, PAYMENT_KEY, REQUESTED_AT);

            attempt.approve(COMPLETED_AT);

            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
            assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
        }

        @Test
        @DisplayName("거절되면 FAILED, 응답이 없으면 UNKNOWN이 된다")
        void failsOrMarksUnknown() {
            PaymentAttempt failed = PaymentAttempt.start(payment, IDEMPOTENCY_KEY, PAYMENT_KEY, REQUESTED_AT);
            PaymentAttempt unknown = PaymentAttempt.start(payment, "idem-2", PAYMENT_KEY, REQUESTED_AT);

            failed.fail(COMPLETED_AT);
            unknown.markUnknown(COMPLETED_AT);

            assertThat(failed.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            assertThat(unknown.getStatus()).isEqualTo(PaymentAttemptStatus.UNKNOWN);
        }

        @Test
        @DisplayName("이미 끝난 시도는 결과를 다시 바꿀 수 없다")
        void rejectsCompletedAttempt() {
            PaymentAttempt attempt = PaymentAttempt.start(payment, IDEMPOTENCY_KEY, PAYMENT_KEY, REQUESTED_AT);
            attempt.approve(COMPLETED_AT);

            assertThatThrownBy(() -> attempt.fail(COMPLETED_AT))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        }

        @Test
        @DisplayName("응답 시각이 없으면 결과를 기록할 수 없다")
        void rejectsNullCompletedAt() {
            PaymentAttempt attempt = PaymentAttempt.start(payment, IDEMPOTENCY_KEY, PAYMENT_KEY, REQUESTED_AT);

            assertThatThrownBy(() -> attempt.approve(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.IN_PROGRESS);
        }
    }
}
