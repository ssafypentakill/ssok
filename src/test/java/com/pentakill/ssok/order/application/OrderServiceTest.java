package com.pentakill.ssok.order.application;

import com.pentakill.ssok.cart.domain.Cart;
import com.pentakill.ssok.cart.domain.CartItem;
import com.pentakill.ssok.cart.domain.CartItemRepository;
import com.pentakill.ssok.cart.domain.CartRepository;
import com.pentakill.ssok.member.domain.ShippingAddress;
import com.pentakill.ssok.member.domain.ShippingAddressRepository;
import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderItem;
import com.pentakill.ssok.order.domain.OrderRepository;
import com.pentakill.ssok.order.infrastructure.persistence.OrderItemJpaRepository;
import com.pentakill.ssok.product.domain.Product;
import com.pentakill.ssok.product.domain.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock OrderItemJpaRepository orderItemRepository;
    @Mock ProductRepository productRepository;
    @Mock ShippingAddressRepository shippingAddressRepository;
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @InjectMocks OrderService orderService;

    @BeforeEach
    void ownedDelivery() {
        when(shippingAddressRepository.findByIdAndMemberId(2L, 1L))
                .thenReturn(Optional.of(mock(ShippingAddress.class)));
    }

    private Product product(long id, long price, long stock) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn("상품 " + id);
        when(product.getPrice()).thenReturn(price);
        when(product.getStatus()).thenReturn("SELLING");
        when(product.getStockQuantity()).thenReturn(stock);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        return product;
    }

    private void savedOrder(long id) {
        Order saved = mock(Order.class);
        when(saved.getId()).thenReturn(id);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);
    }

    @Test
    void directPurchaseAtFiftyThousandHasFreeShippingAndSavesPriceSnapshot() {
        product(10L, 25_000L, 2);
        savedOrder(90L);
        when(productRepository.decreaseStockIfAvailable(10L, 2)).thenReturn(1);

        Long orderId = orderService.createDirectOrder(1L, 2L, 10L, 2);

        assertEquals(90L, orderId);
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertEquals(0L, order.getValue().getShippingFee());
        assertEquals(50_000L, order.getValue().getTotalAmount());
        ArgumentCaptor<OrderItem> item = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(item.capture());
        assertEquals(90L, item.getValue().getOrderId());
        assertEquals(10L, item.getValue().getProductId());
        assertEquals(2, item.getValue().getQuantity());
        assertEquals("상품 10", item.getValue().getProductName());
        assertEquals(25_000L, item.getValue().getUnitPrice());
        verify(productRepository).decreaseStockIfAvailable(10L, 2);
    }

    @Test
    void directPurchaseBelowFiftyThousandAddsThreeThousandShipping() {
        product(10L, 24_000L, 2);
        savedOrder(90L);
        when(productRepository.decreaseStockIfAvailable(10L, 2)).thenReturn(1);

        orderService.createDirectOrder(1L, 2L, 10L, 2);

        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertEquals(3_000L, order.getValue().getShippingFee());
        assertEquals(51_000L, order.getValue().getTotalAmount());
    }

    @Test
    void stockChangeFailureDoesNotSaveOrderItem() {
        product(10L, 25_000L, 2);
        when(orderRepository.save(any(Order.class))).thenReturn(mock(Order.class));
        when(productRepository.decreaseStockIfAvailable(10L, 2)).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createDirectOrder(1L, 2L, 10L, 2));

        verify(orderItemRepository, never()).save(any());
    }

    @Test
    void cartPurchaseSumsSelectedItemsAndDeletesOnlyThoseItems() {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(7L);
        when(cartRepository.findByUserId(1L)).thenReturn(cart);
        CartItem first = new CartItem(7L, 10L, 2);
        CartItem second = new CartItem(7L, 11L, 1);
        when(cartItemRepository.findByIdAndCartId(100L, 7L)).thenReturn(Optional.of(first));
        when(cartItemRepository.findByIdAndCartId(101L, 7L)).thenReturn(Optional.of(second));
        product(10L, 10_000L, 2);
        product(11L, 30_000L, 1);
        savedOrder(91L);
        when(productRepository.decreaseStockIfAvailable(10L, 2)).thenReturn(1);
        when(productRepository.decreaseStockIfAvailable(11L, 1)).thenReturn(1);

        Long orderId = orderService.createCartOrder(1L, 2L, List.of(100L, 101L));

        assertEquals(91L, orderId);
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertEquals(50_000L, order.getValue().getTotalAmount());
        assertEquals(0L, order.getValue().getShippingFee());
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));
        verify(cartItemRepository).deleteAll(List.of(first, second));
        verify(cartItemRepository, never()).deleteAllByCartId(anyLong());
    }

    @Test
    void cartPurchaseRejectsItemOutsideMembersCart() {
        Cart cart = mock(Cart.class);
        when(cart.getId()).thenReturn(7L);
        when(cartRepository.findByUserId(1L)).thenReturn(cart);
        when(cartItemRepository.findByIdAndCartId(100L, 7L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> orderService.createCartOrder(1L, 2L, List.of(100L)));

        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).decreaseStockIfAvailable(anyLong(), anyLong());
    }
}
