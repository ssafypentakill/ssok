package com.pentakill.ssok.payment.infrastructure.order;

import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderRepository;
import com.pentakill.ssok.order.domain.OrderStatus;
import com.pentakill.ssok.payment.application.OrderInfo;
import com.pentakill.ssok.payment.application.OrderReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderReaderAdapter implements OrderReader {

    private final OrderRepository orderRepository;

    @Override
    public Optional<OrderInfo> findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> !order.getDeleted())
                .map(this::toOrderInfo);
    }

    private OrderInfo toOrderInfo(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getMemberId(),
                order.getTotalAmount(),
                order.getStatus() == OrderStatus.PENDING_PAYMENT
        );
    }
}
