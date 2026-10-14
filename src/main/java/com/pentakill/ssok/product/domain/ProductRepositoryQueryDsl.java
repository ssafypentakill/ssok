package com.pentakill.ssok.product.domain;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.pentakill.ssok.product.domain.QProduct.product;


@Repository
@RequiredArgsConstructor
public class ProductRepositoryQueryDsl {
    private final JPAQueryFactory jpaQueryFactory;

    public List<Product> searchProducts(String productName, String brand, String category, String sort, String order, Long lastSeenId, int size) {

        List<Product> productList;
        JPAQuery<Product> jpaQuery = jpaQueryFactory.selectFrom(product);
        jpaQuery.where(product.status.ne("DELETED"));

        if (!productName.equals("")) {
            jpaQuery.where(product.name.contains(productName));
        }
        if (!brand.equals("")) {
            jpaQuery.where(product.brand.eq(brand));
        }
        if (!category.equals("all")) {
            jpaQuery.where(product.category.eq(category));
        }
        if (sort.equals("id")) {
            if (order.equals("asc")) {
                if (lastSeenId != null) {
                    jpaQuery.where(product.id.gt(lastSeenId));
                }
                jpaQuery.orderBy(product.id.asc());
            } else {
                if (lastSeenId != null) {
                    jpaQuery.where(product.id.lt(lastSeenId));
                }
                jpaQuery.orderBy(product.id.desc());
            }
        }
        jpaQuery.limit(size);
        productList = jpaQuery.fetch();
        return productList;


    }

}
