package com.pentakill.ssok.wishlist.application;

import com.pentakill.ssok.product.domain.Product;
import com.pentakill.ssok.product.domain.ProductRepository;
import com.pentakill.ssok.wishlist.domain.Wishlist;
import com.pentakill.ssok.wishlist.domain.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    @Transactional
    public boolean toggleWishlist(long productId, long userId) {

        Wishlist existingWishlist = wishlistRepository.findByUserIdAndProductId(userId, productId);

        if (existingWishlist == null) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new NullPointerException("Product with id: " + productId + " does not exist"));
            if (!product.getStatus().equals("SELLING")){
                throw new IllegalStateException("Product with id: " + productId + " is not SELLING");
            }
            wishlistRepository.save(Wishlist.builder()
                    .productId(productId)
                    .userId(userId)
                    .build());
            return true;

        } else{
            wishlistRepository.delete(existingWishlist);
            return false;
        }
    }

}
