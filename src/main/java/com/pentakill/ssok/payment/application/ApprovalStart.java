package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;

// 승인 시작의 결과.
// 처음 온 요청이면 PG를 부른 뒤 결과를 저장할 시도 id를 담고,
// 같은 멱등키로 다시 온 요청이면 PG를 부르지 않도록 replayed를 true로 담는다.
public record ApprovalStart(Payment payment, Long attemptId, boolean replayed) {

    public static ApprovalStart started(Payment payment, Long attemptId) {
        return new ApprovalStart(payment, attemptId, false);
    }

    public static ApprovalStart replayed(Payment payment) {
        return new ApprovalStart(payment, null, true);
    }
}
