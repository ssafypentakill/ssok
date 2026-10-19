package com.pentakill.ssok.order.infrastructure.persistence;

import com.pentakill.ssok.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {


}
