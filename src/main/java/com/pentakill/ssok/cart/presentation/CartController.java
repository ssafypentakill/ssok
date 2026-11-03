package com.pentakill.ssok.cart.presentation;

import com.pentakill.ssok.cart.application.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto.Response> getCart(
            @RequestParam("memberId") long memberId
    ){

        return ResponseEntity.ok(cartService.getCart(memberId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto.Response> addItem(
            @RequestParam("memberId") Long memberId,
            @RequestBody CartDto.AddRequest request
    ) {
        return ResponseEntity.ok(
                cartService.addCartItem(
                        memberId,
                        request.productId(),
                        request.quantity()
                )
        );
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<CartDto.Response> updateQuantity(
            @RequestParam("memberId") Long memberId,
            @PathVariable("cartItemId") Long cartItemId,
            @RequestBody CartDto.UpdateQuantityRequest request
    ) {
        return ResponseEntity.ok(
                cartService.updateQuantity(
                        memberId,
                        cartItemId,
                        request.quantity()
                )
        );
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartDto.Response> deleteItem(
            @RequestParam("memberId") Long memberId,
            @PathVariable("cartItemId") Long cartItemId
    ) {
        return ResponseEntity.ok(
                cartService.deleteCartItem(memberId, cartItemId)
        );
    }

    // 장바구니 전체 비우기
    @DeleteMapping("/items")
    public ResponseEntity<CartDto.Response> deleteAllItems(
            @RequestParam("memberId") Long memberId
    ) {
        return ResponseEntity.ok(
                cartService.deleteAllItems(memberId)
        );
    }

}
