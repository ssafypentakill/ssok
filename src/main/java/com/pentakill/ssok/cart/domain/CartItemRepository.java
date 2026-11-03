package com.pentakill.ssok.cart.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByCartId(Long id);

    CartItem findByCartIdAndProductId(Long cartId, Long productId);

    void deleteAllByCartId(Long cartId);

    Optional<CartItem> findByIdAndCartId(Long cartItemId, Long id);
}
