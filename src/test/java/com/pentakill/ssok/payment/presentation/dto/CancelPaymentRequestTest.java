package com.pentakill.ssok.payment.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CancelPaymentRequestTest {

    @Test
    @DisplayName("취소 사유가 있으면 요청을 만들 수 있다")
    void createsRequest() {
        CancelPaymentRequest request = new CancelPaymentRequest("고객 요청");

        assertThat(request.cancelReason()).isEqualTo("고객 요청");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("취소 사유가 비어 있으면 요청을 만들 수 없다")
    void rejectsBlankReason(String cancelReason) {
        assertThatThrownBy(() -> new CancelPaymentRequest(cancelReason))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
