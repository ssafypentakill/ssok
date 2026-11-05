package com.pentakill.ssok.order.domain;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findAllByMemberIdAndDeletedFalse(Long memberId);

}
