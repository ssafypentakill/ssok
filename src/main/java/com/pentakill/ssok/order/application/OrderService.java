package com.pentakill.ssok.order.application;

import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @Transactional
    public Long createOrder(
            Long memberId,
            Long deliveryId,
            Long orderItemId,
            long shippingFee,
            long totalAmount
    ) {
        if (memberId == null || deliveryId == null || orderItemId == null) {
            throw new IllegalArgumentException("회원 ID, 배송지 ID, 주문상품 ID는 필수입니다. ");
        }
        if (shippingFee < 0 || totalAmount < shippingFee || totalAmount < 0) {
            throw new IllegalArgumentException("주문 금액이 올바르지 않습니다.");
        }

        Order order = Order.create(memberId, deliveryId, orderItemId ,shippingFee, totalAmount);


        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }

    public Order getOrder(Long orderId, Long memberId) {
        if(orderId == null || memberId == null) {
            throw new IllegalArgumentException("주문 ID와 회원 ID는 필수입니다.");
        }
        Order order = orderRepository.findById(orderId).orElseThrow(()->new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if(!order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 주문만 조회할 수 있습니다.");
        }


        return order;
    }


}
