package com.pentakill.ssok.payment.infrastructure.pg;

import static org.assertj.core.api.Assertions.assertThat;

import com.pentakill.ssok.payment.application.PgApprovalResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FakePaymentGatewayTest {

    private static final String PAYMENT_KEY = "pay_test_key";
    private static final long ORDER_ID = 1L;
    private static final long AMOUNT = 30_000L;

    @ParameterizedTest
    @CsvSource({
            "0.00, UNKNOWN",
            "0.09, UNKNOWN",
            "0.10, REJECTED",
            "0.19, REJECTED",
            "0.20, APPROVED",
            "0.99, APPROVED"
    })
    @DisplayName("뽑힌 숫자가 어느 구간에 있는지에 따라 결과가 정해진다")
    void decidesResultByChance(double chance, PgApprovalResult expected) {
        FakePaymentGateway gateway = new FakePaymentGateway(0.1, 0.1, 0, () -> chance);

        PgApprovalResult result = gateway.approve(PAYMENT_KEY, ORDER_ID, AMOUNT);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("비율이 모두 0이면 항상 승인한다")
    void alwaysApprovesWhenRatesAreZero() {
        FakePaymentGateway gateway = new FakePaymentGateway(0, 0, 0, () -> 0.0);

        PgApprovalResult result = gateway.approve(PAYMENT_KEY, ORDER_ID, AMOUNT);

        assertThat(result).isEqualTo(PgApprovalResult.APPROVED);
    }
}
