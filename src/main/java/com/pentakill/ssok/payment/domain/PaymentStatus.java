package com.pentakill.ssok.payment.domain;

public enum PaymentStatus {
    READY, //결제 준비됨 (금액 확정)
    IN_PROGRESS, //PG에 승인 요청 중
    APPROVED, //승인 완료
    FAILED, //승인 실패 (PG 거절)
    UNKNOWN, //결과를 모름 (타임아웃)
    CANCELED //취소 완료
}
