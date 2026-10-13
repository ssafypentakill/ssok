package com.pentakill.ssok.product.application;

import com.pentakill.ssok.product.domain.Product;
import com.pentakill.ssok.product.domain.ProductRepository;
import com.pentakill.ssok.product.domain.ProductRepositoryQueryDsl;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    final ProductRepository productRepository;
    private final ProductRepositoryQueryDsl productRepositoryQueryDsl;

    //상품 상세페이지
    public Product getProduct(long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        if (product.getStatus().equals("DELETED")) {
            throw new EntityNotFoundException("Product with id: " + productId + " is deleted");
        }
        return product;
    }

    //상품 리스트 검색 및 정렬
    public List<Product> getProducts(String productName, String brand, String category, String sort, String order, Long lastSeenId, int size) {
        return productRepositoryQueryDsl.searchProducts(productName, brand, category, sort, order, lastSeenId, size);
    }

    //상품 등록?
    public Product create(Product product) {
        return productRepository.save(product);
    }
    //상품 변경
    public long update(long productId, Product input) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("없는 상품 입니다."));
        product.update(input.getName(), input.getDescription(), input.getBrand(), input.getCategory(), input.getPrice(), input.getImageUrl());
        productRepository.save(product);
        return productId;
    }
    //상품 삭제(soft delete)
    public void delete(long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException("없는 상품 입니다."));
        product.delete();
        productRepository.save(product);

    }



}
