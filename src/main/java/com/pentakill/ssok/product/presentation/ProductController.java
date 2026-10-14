package com.pentakill.ssok.product.presentation;

import com.pentakill.ssok.product.application.ProductService;
import com.pentakill.ssok.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable long productId){
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }
    @GetMapping("/")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(required = false, defaultValue = "") String productName,
                                                        @RequestParam(required = false, defaultValue = "") String brand,
                                                        @RequestParam(required = true, defaultValue = "all") String category,
                                                        @RequestParam(required = true, defaultValue = "id") String sort,
                                                        @RequestParam(required = true, defaultValue = "desc") String order,
                                                        @RequestParam(required = false) Long lastSeenId,
                                                        @RequestParam(required = true) int size
                                                        ){
        List<Product> list = productService.getProducts(productName, brand, category, sort, order, lastSeenId, size);
        return ResponseEntity.ok(list);
    }
}
