package com.pentakill.ssok.order.presentation;

import com.pentakill.ssok.order.application.OrderService;
import com.pentakill.ssok.order.presentation.dto.OrderCreateRequest;
import com.pentakill.ssok.order.presentation.dto.OrderCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCreateResponse createOrder(@RequestBody OrderCreateRequest request) {
        Long orderId = orderService.createOrder(
                request.memberId(),
                request.deliveryId(),
                request.orderItemId(),
                request.shippingFee(),
                request.totalAmount()


        );

        return new OrderCreateResponse(orderId);
    }
}
