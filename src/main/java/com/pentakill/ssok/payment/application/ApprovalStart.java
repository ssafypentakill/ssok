package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;

// 승인 시작의 결과. PG를 부른 뒤 어떤 시도의 결과를 저장할지 알 수 있도록 시도 id를 함께 돌려준다.
public record ApprovalStart(Payment payment, Long attemptId) {
}
