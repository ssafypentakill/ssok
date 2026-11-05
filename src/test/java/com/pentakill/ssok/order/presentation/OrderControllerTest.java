package com.pentakill.ssok.order.presentation;

import com.pentakill.ssok.common.exception.GlobalExceptionHandler;
import com.pentakill.ssok.order.application.OrderService;
import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderItem;
import com.pentakill.ssok.order.domain.OrderStatus;
import com.pentakill.ssok.payment.application.PaymentCancelService;
import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {
    private OrderService orderService;
    private PaymentCancelService cancellationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        cancellationService = mock(PaymentCancelService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, cancellationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void directPurchaseReceivesProductAndQuantityAndReturnsOrderId() throws Exception {
        when(orderService.createDirectOrder(1L, 2L, 10L, 3)).thenReturn(90L);

        mockMvc.perform(post("/api/orders/direct")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryId":2,"productId":10,"quantity":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(90));

        verify(orderService).createDirectOrder(1L, 2L, 10L, 3);
    }

    @Test
    void cartPurchasePassesOnlySelectedCartItemIds() throws Exception {
        when(orderService.createCartOrder(1L, 2L, List.of(100L, 101L))).thenReturn(91L);

        mockMvc.perform(post("/api/orders/cart")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryId":2,"cartItemIds":[100,101]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(91));

        verify(orderService).createCartOrder(1L, 2L, List.of(100L, 101L));
    }

    @Test
    void orderListReturnsPurchasedItemsAndOrderTotal() throws Exception {
        Order order = order(90L);
        OrderItem item = OrderItem.create(90L, 10L, 2, "사과", 25_000L);
        when(orderService.getOrdersByMember(1L)).thenReturn(List.of(order));
        when(orderService.getOrderItems(90L, 1L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/orders").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(90))
                .andExpect(jsonPath("$[0].totalAmount").value(50_000))
                .andExpect(jsonPath("$[0].items[0].productName").value("사과"))
                .andExpect(jsonPath("$[0].items[0].quantity").value(2))
                .andExpect(jsonPath("$[0].items[0].subtotal").value(50_000));

        verify(orderService).getOrdersByMember(1L);
        verify(orderService).getOrderItems(90L, 1L);
    }

    @Test
    void orderDetailReturnsOnlyRequestedOrder() throws Exception {
        Order order = order(90L);
        when(orderService.getOrder(90L, 1L)).thenReturn(order);
        when(orderService.getOrderItems(90L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/90").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(90))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.items").isArray());

        verify(orderService).getOrder(90L, 1L);
        verify(orderService).getOrderItems(90L, 1L);
    }

    @Test
    void paidOrderCancellationReturnsCanceledPayment() throws Exception {
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(7L);
        when(payment.getOrderId()).thenReturn(90L);
        when(payment.getAmount()).thenReturn(50_000L);
        when(payment.getStatus()).thenReturn(PaymentStatus.CANCELED);
        when(cancellationService.cancelByOrderId(90L, 1L, "단순 변심")).thenReturn(payment);

        mockMvc.perform(post("/api/orders/90/cancel")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"단순 변심"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(90))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(cancellationService).cancelByOrderId(90L, 1L, "단순 변심");
    }

    @Test
    void missingMemberHeaderIsRejected() throws Exception {
        mockMvc.perform(get("/api/orders/90"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderService, cancellationService);
    }

    @Test
    void serviceValidationErrorBecomesBadRequest() throws Exception {
        when(orderService.createCartOrder(1L, 2L, List.of(100L)))
                .thenThrow(new IllegalArgumentException("본인 장바구니의 상품이 아닙니다."));

        mockMvc.perform(post("/api/orders/cart")
                        .header("X-Member-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryId":2,"cartItemIds":[100]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인 장바구니의 상품이 아닙니다."));
    }

    private Order order(long id) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getMemberId()).thenReturn(1L);
        when(order.getDeliveryId()).thenReturn(2L);
        when(order.getStatus()).thenReturn(OrderStatus.PENDING_PAYMENT);
        when(order.getOrderAt()).thenReturn(LocalDateTime.of(2026, 11, 4, 12, 0));
        when(order.getTotalAmount()).thenReturn(50_000L);
        return order;
    }
}
