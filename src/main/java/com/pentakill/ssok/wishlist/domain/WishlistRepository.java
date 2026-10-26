package com.pentakill.ssok.wishlist.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist,Long> {
    Wishlist findByUserIdAndProductId(long userId, long productId);
}
