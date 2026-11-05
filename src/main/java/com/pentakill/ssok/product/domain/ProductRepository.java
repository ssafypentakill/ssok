package com.pentakill.ssok.product.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity - :quantity " +
           "where p.id = :productId and p.stockQuantity >= :quantity and p.status = 'SELLING'")
    int decreaseStockIfAvailable(@Param("productId") Long productId,
                                 @Param("quantity") long quantity);

    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :quantity where p.id = :productId")
    int increaseStock(@Param("productId") Long productId,
                      @Param("quantity") long quantity);
}
