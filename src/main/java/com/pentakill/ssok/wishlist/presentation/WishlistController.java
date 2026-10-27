package com.pentakill.ssok.wishlist.presentation;

import com.pentakill.ssok.wishlist.application.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    public boolean createWishlist(@PathVariable long productId, @AuthenticationPrincipal long userId) {
        return wishlistService.toggleWishlist(productId, userId);
    }
}
