package com.pentakill.ssok.payment.application;

public record OrderInfo(Long orderId, Long memberId, long amount, boolean payable) {
    public boolean isOrderedBy(Long requestMemberId) {
        return memberId.equals(requestMemberId);
    }
}
