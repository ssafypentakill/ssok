package com.pentakill.ssok.payment.infrastructure.order;

import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderItem;
import com.pentakill.ssok.order.domain.OrderRepository;
import com.pentakill.ssok.order.domain.OrderStatus;
import com.pentakill.ssok.order.infrastructure.persistence.OrderItemJpaRepository;
import com.pentakill.ssok.product.domain.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderPaymentUpdaterAdapterTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderItemJpaRepository orderItemRepository = mock(OrderItemJpaRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderPaymentUpdaterAdapter adapter =
            new OrderPaymentUpdaterAdapter(orderRepository, orderItemRepository, productRepository);

    @Test
    void approvedPaymentMarksTheOrderPaid() {
        Order order = Order.create(1L, 2L, 0, 50_000L);
        when(orderRepository.findById(90L)).thenReturn(Optional.of(order));

        adapter.markPaid(90L);

        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void cancellationRestoresEveryPurchasedQuantity() {
        Order order = Order.create(1L, 2L, 0, 50_000L);
        order.markPaid();
        when(orderRepository.findById(90L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderIdOrderByIdAsc(90L)).thenReturn(List.of(
                OrderItem.create(90L, 10L, 2, "사과", 10_000L),
                OrderItem.create(90L, 11L, 1, "배", 30_000L)));
        when(productRepository.increaseStock(10L, 2)).thenReturn(1);
        when(productRepository.increaseStock(11L, 1)).thenReturn(1);

        adapter.cancelAndRestore(90L, 1L);

        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(productRepository).increaseStock(10L, 2);
        verify(productRepository).increaseStock(11L, 1);
    }

    @Test
    void pendingOrderCannotBeCanceled() {
        Order order = Order.create(1L, 2L, 0, 50_000L);
        when(orderRepository.findById(90L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> adapter.cancelAndRestore(90L, 1L));

        verifyNoInteractions(orderItemRepository, productRepository);
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }
}
