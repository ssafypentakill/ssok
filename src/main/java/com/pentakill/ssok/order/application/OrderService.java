package com.pentakill.ssok.order.application;

import com.pentakill.ssok.cart.domain.Cart;
import com.pentakill.ssok.cart.domain.CartItem;
import com.pentakill.ssok.cart.domain.CartItemRepository;
import com.pentakill.ssok.cart.domain.CartRepository;
import com.pentakill.ssok.member.domain.ShippingAddressRepository;
import com.pentakill.ssok.order.domain.Order;
import com.pentakill.ssok.order.domain.OrderItem;
import com.pentakill.ssok.order.domain.OrderRepository;
import com.pentakill.ssok.order.infrastructure.persistence.OrderItemJpaRepository;
import com.pentakill.ssok.product.domain.Product;
import com.pentakill.ssok.product.domain.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // 단일 상품의 현재 가격과 재고를 확인하고 주문 ID를 반환한다.
    @Transactional
    public Long createDirectOrder(Long memberId, Long deliveryId, Long productId, int quantity) {
        validateDeliveryAddress(deliveryId, memberId);
        PurchaseLine line = prepareLine(productId, quantity);
        return saveOrder(memberId, deliveryId, List.of(line));
    }

    // 선택한 장바구니 상품만 주문하고 구매 완료 시 해당 항목을 삭제한다.
    @Transactional
    public Long createCartOrder(Long memberId, Long deliveryId, List<Long> cartItemIds) {
        validateDeliveryAddress(deliveryId, memberId);
        List<CartItem> selectedItems = getSelectedCartItems(memberId, cartItemIds);
        List<PurchaseLine> lines = new ArrayList<>();
        for (CartItem item : selectedItems) {
            lines.add(prepareLine(item.getProductId(), item.getQuantity()));
        }
        Long orderId = saveOrder(memberId, deliveryId, lines);
        cartItemRepository.deleteAll(selectedItems);
        return orderId;
    }

    // 전체 상품 금액을 계산해 주문과 주문상품을 저장하고 재고를 차감한다.
    private Long saveOrder(Long memberId, Long deliveryId, List<PurchaseLine> lines) {
        long productTotal = 0;
        for (PurchaseLine line : lines) {
            productTotal = Math.addExact(productTotal,
                    Math.multiplyExact(line.unitPrice(), (long) line.quantity()));
        }
        long shippingFee = calculateShippingFee(productTotal);
        long totalAmount = Math.addExact(productTotal, shippingFee);
        Order order = orderRepository.save(Order.create(memberId, deliveryId, shippingFee, totalAmount));

        for (PurchaseLine line : lines) {
            int updated = productRepository.decreaseStockIfAvailable(line.productId(), line.quantity());
            if (updated != 1) {
                throw new IllegalArgumentException("판매 중이 아니거나 상품 재고가 부족합니다.");
            }
            orderItemRepository.save(OrderItem.create(order.getId(), line.productId(),
                    line.quantity(), line.productName(), line.unitPrice()));
        }
        return order.getId();
    }

    // 삭제되지 않은 회원 주문 목록을 조회한다.
    public List<Order> getOrdersByMember(Long memberId) {
        requireMemberId(memberId);
        return orderRepository.findAllByMemberIdAndDeletedFalse(memberId);
    }

    // 회원 본인의 주문인지 확인한 뒤 한 건을 조회한다.
    public Order getOrder(Long orderId, Long memberId) {
        requireMemberId(memberId);
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("주문 ID가 올바르지 않습니다.");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
        if (Boolean.TRUE.equals(order.getDeleted()) || !order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        }
        return order;
    }

    // 주문 소유권을 확인한 뒤 주문상품 목록을 조회한다.
    public List<OrderItem> getOrderItems(Long orderId, Long memberId) {
        getOrder(orderId, memberId);
        return orderItemRepository.findAllByOrderIdOrderByIdAsc(orderId);
    }

    // 상품의 주문 가능 여부를 확인하고 주문 당시 이름과 단가를 기록한다.
    private PurchaseLine prepareLine(Long productId, int quantity) {
        Product product = getOrderableProduct(productId, quantity);
        if (product.getPrice() < 0) {
            throw new IllegalArgumentException("상품 가격이 올바르지 않습니다.");
        }
        Math.multiplyExact(product.getPrice(), (long) quantity);
        return new PurchaseLine(product.getId(), quantity, product.getName(), product.getPrice());
    }

    // 판매 상태와 현재 재고를 검사한다.
    private Product getOrderableProduct(Long productId, int quantity) {
        if (productId == null || productId <= 0 || quantity <= 0) {
            throw new IllegalArgumentException("상품 ID와 1개 이상의 수량이 필요합니다.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        if (!"SELLING".equals(product.getStatus())) {
            throw new IllegalArgumentException("판매 중인 상품만 주문할 수 있습니다.");
        }
        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("상품 재고가 부족합니다.");
        }
        return product;
    }

    // 상품 합계가 5만 원 이상이면 배송비를 면제한다.
    private long calculateShippingFee(long productTotal) {
        return productTotal >= 50_000 ? 0 : 3_000;
    }

    // 배송지가 해당 회원의 것인지 확인한다.
    private void validateDeliveryAddress(Long deliveryId, Long memberId) {
        requireMemberId(memberId);
        if (deliveryId == null || deliveryId <= 0) {
            throw new IllegalArgumentException("배송지 ID가 올바르지 않습니다.");
        }
        shippingAddressRepository.findByIdAndMemberId(deliveryId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("본인의 배송지를 찾을 수 없습니다."));
    }

    private void requireMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("회원 ID가 올바르지 않습니다.");
        }
    }

    // 중복 ID를 거르고 본인 장바구니의 선택 항목만 가져온다.
    private List<CartItem> getSelectedCartItems(Long memberId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new IllegalArgumentException("구매할 장바구니 상품이 없습니다.");
        }
        Set<Long> seen = new HashSet<>();
        for (Long cartItemId : cartItemIds) {
            if (cartItemId == null || cartItemId <= 0 || !seen.add(cartItemId)) {
                throw new IllegalArgumentException("장바구니 상품 ID가 올바르지 않습니다.");
            }
        }
        Cart cart = cartRepository.findByUserId(memberId);
        if (cart == null) {
            throw new IllegalArgumentException("장바구니가 없습니다.");
        }
        List<CartItem> selectedItems = new ArrayList<>();
        for (Long cartItemId : cartItemIds) {
            selectedItems.add(cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                    .orElseThrow(() -> new IllegalArgumentException("본인 장바구니의 상품이 아닙니다.")));
        }
        return selectedItems;
    }

    private record PurchaseLine(Long productId, int quantity, String productName, long unitPrice) {}
}
