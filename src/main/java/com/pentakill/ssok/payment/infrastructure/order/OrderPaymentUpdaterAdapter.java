package com.pentakill.ssok.payment.infrastructure.order;

import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderItem;
import com.pentakill.ssok.order.domain.OrderRepository;
import com.pentakill.ssok.order.domain.OrderStatus;
import com.pentakill.ssok.order.infrastructure.persistence.OrderItemJpaRepository;
import com.pentakill.ssok.payment.application.OrderPaymentUpdater;
import com.pentakill.ssok.product.domain.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentUpdaterAdapter implements OrderPaymentUpdater {
    private final OrderRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final ProductRepository productRepository;

    // 결제 승인에 맞춰 주문을 PAID로 변경한다.
    @Override
    public void markPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));
        order.markPaid();
    }

    // 본인의 결제 완료 주문인지 확인한다.
    @Override
    public void requirePaidOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
                .filter(found -> found.getMemberId().equals(memberId))
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));
        if (Boolean.TRUE.equals(order.getDeleted()) || order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("결제 완료된 주문만 취소할 수 있습니다.");
        }
    }

    // 주문을 취소하고 각 주문상품의 수량을 재고에 되돌린다.
    @Override
    public void cancelAndRestore(Long orderId, Long memberId) {
        requirePaidOrder(orderId, memberId);
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.markCanceled();
        for (OrderItem item : orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId)) {
            int updated = productRepository.increaseStock(item.getProductId(), item.getQuantity());
            if (updated != 1) {
                throw new IllegalStateException("취소 주문의 상품 재고를 복구할 수 없습니다.");
            }
        }
    }
}
