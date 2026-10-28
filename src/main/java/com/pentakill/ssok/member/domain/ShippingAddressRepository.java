package com.pentakill.ssok.member.domain;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressRepository {

    ShippingAddress saveAndFlush(ShippingAddress shippingAddress);

    List<ShippingAddress> findAllByMemberId(Long memberId);

    Optional<ShippingAddress> findByIdAndMemberId(Long id, Long memberId);

    void delete(ShippingAddress shippingAddress);
}
