package com.pentakill.ssok.order.infrastructure.persistence;

import com.pentakill.ssok.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByMemberIdAndDeletedFalse(Long memberId);

}
