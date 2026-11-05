package com.pentakill.ssok.order.presentation;

import com.pentakill.ssok.order.application.OrderService;
import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.presentation.dto.CartOrderRequest;
import com.pentakill.ssok.order.presentation.dto.CancelOrderRequest;
import com.pentakill.ssok.payment.application.PaymentCancelService;
import com.pentakill.ssok.payment.presentation.dto.PaymentResponse;
import com.pentakill.ssok.order.presentation.dto.DirectOrderRequest;
import com.pentakill.ssok.order.presentation.dto.OrderCreateResponse;
import com.pentakill.ssok.order.presentation.dto.OrderDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final PaymentCancelService paymentCancelService;

    // 단일 상품 구매 요청을 주문 서비스에 전달한다.
    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreateResponse createDirectOrder(@RequestHeader("X-Member-Id") Long memberId,
                                                 @RequestBody DirectOrderRequest request) {
        return new OrderCreateResponse(orderService.createDirectOrder(
                memberId, request.deliveryId(), request.productId(), request.quantity()));
    }

    // 선택한 장바구니 항목의 구매 요청을 전달한다.
    @PostMapping("/cart")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreateResponse createCartOrder(@RequestHeader("X-Member-Id") Long memberId,
                                               @RequestBody CartOrderRequest request) {
        return new OrderCreateResponse(orderService.createCartOrder(
                memberId, request.deliveryId(), request.cartItemIds()));
    }

    // 결제 완료 주문의 취소 사유를 받아 취소 결과를 반환한다.
    @PostMapping("/{orderId}/cancel")
    public PaymentResponse cancelOrder(@RequestHeader("X-Member-Id") Long memberId,
                                       @PathVariable Long orderId,
                                       @RequestBody CancelOrderRequest request) {
        return PaymentResponse.from(paymentCancelService.cancelByOrderId(
                orderId, memberId, request.reason()));
    }

    // 회원의 주문 내역과 주문상품을 목록으로 반환한다.
    @GetMapping
    public List<OrderDetailResponse> getOrders(@RequestHeader("X-Member-Id") Long memberId) {
        return orderService.getOrdersByMember(memberId).stream()
                .map(order -> OrderDetailResponse.from(order,
                        orderService.getOrderItems(order.getId(), memberId)))
                .toList();
    }

    // 주문 한 건의 상세 정보와 주문상품을 반환한다.
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@RequestHeader("X-Member-Id") Long memberId,
                                        @PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId, memberId);
        return OrderDetailResponse.from(order, orderService.getOrderItems(orderId, memberId));
    }
}
