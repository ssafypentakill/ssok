package com.pentakill.ssok.cart.application;

import com.pentakill.ssok.cart.domain.Cart;
import com.pentakill.ssok.cart.domain.CartItem;
import com.pentakill.ssok.cart.domain.CartItemRepository;
import com.pentakill.ssok.cart.domain.CartRepository;
import com.pentakill.ssok.cart.presentation.CartDto;
import com.pentakill.ssok.product.domain.Product;
import com.pentakill.ssok.product.domain.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartDto.Response getCart(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("유효한 회원 ID가 필요합니다.");
        }
        Cart cart = cartRepository.findByUserId(memberId);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartDto.Response addCartItem(Long memberId, Long productId, Integer quantity) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("유효한 회원 ID가 필요합니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량을 확인하세요");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("유효한 상품 ID가 필요합니다.");
        }
        Product product = productRepository.findById(productId).orElseThrow(() -> new EntityNotFoundException("상품이 없습니다"));
        Cart cart = cartRepository.findByUserId(memberId);
        if (cart == null) {
            cart = cartRepository.save(new Cart(memberId));
        }
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);
        int finalQuantity = item == null ? quantity : item.getQuantity() + quantity;

        if (item == null) {
            cartItemRepository.save(
                    new CartItem(cart.getId(), productId, finalQuantity)
            );
        } else {
            item.changeQuantity(finalQuantity);
        }

        return buildCartResponse(cart);

    }

    @Transactional
    public CartDto.Response updateQuantity(Long memberId, Long cartItemId, Integer quantity) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("유효한 회원 ID가 필요합니다.");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("수량을 확인하세요");
        }
        Cart cart = cartRepository.findByUserId(memberId);
        if (cart == null) {
            throw new EntityNotFoundException("장바구니가 없습니다.");
        }
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId()).orElseThrow(() -> new EntityNotFoundException("없는 장바구니 항목입니다"));
        Product product = productRepository.findById(item.getProductId()).orElseThrow(() -> new EntityNotFoundException("상품이 없습니다"));
        item.changeQuantity(quantity);
        return buildCartResponse(cart);
    }

    @Transactional
    public CartDto.Response deleteCartItem(Long memberId, Long cartItemId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("유효한 회원 ID가 필요합니다.");
        }
        Cart cart = cartRepository.findByUserId(memberId);
        if (cart == null) {
            throw new EntityNotFoundException("장바구니가 없습니다.");
        }
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId()).orElseThrow(() -> new EntityNotFoundException("없는 장바구니 항목입니다"));
        cartItemRepository.delete(item);
        return buildCartResponse(cart);
    }
    @Transactional
    public CartDto.Response deleteAllItems(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("유효한 회원 ID가 필요합니다.");
        }
        Cart cart = cartRepository.findByUserId(memberId);
        if (cart == null) {
            throw new EntityNotFoundException("장바구니가 없습니다.");
        }
        cartItemRepository.deleteAllByCartId(cart.getId());
        return buildCartResponse(cart);
    }
    private CartDto.Response buildCartResponse(Cart cart) {

        if (cart == null) {
            return new CartDto.Response(null, List.of(), 0);
        }
        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            return new CartDto.Response(cart.getId(), List.of(), 0);
        }
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();
        List<Product> productList =
                productRepository.findAllById(productIds);

        Map<Long, Product> products = new HashMap<>();

        for (Product product : productList) {
            products.put(product.getId(), product);
        }

        List<CartDto.ItemResponse> items = cartItems.stream()
                .map(item -> {
                    Product product = products.get(item.getProductId());

                    if (product == null) {
                        throw new IllegalStateException(
                                "상품 정보를 찾을 수 없습니다: " + item.getProductId()
                        );
                    }

                    long subtotal = Math.multiplyExact(
                            product.getPrice(), (long) item.getQuantity()
                    );

                    return new CartDto.ItemResponse(
                            item.getId(),
                            item.getProductId(),
                            product.getName(),
                            product.getPrice(),
                            item.getQuantity(),
                            subtotal
                    );

                }).toList();

        long totalPrice = items.stream()
                .mapToLong(CartDto.ItemResponse::subtotal)
                .reduce(0L, Math::addExact);

        return new CartDto.Response(
                cart.getId(),
                items,
                totalPrice
        );

    }
}
