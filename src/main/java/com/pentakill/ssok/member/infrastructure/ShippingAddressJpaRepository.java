package com.pentakill.ssok.member.infrastructure.persistence;

import com.pentakill.ssok.member.domain.ShippingAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingAddressJpaRepository
        extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findAllByMemberId(Long memberId);

    Optional<ShippingAddress> findByIdAndMemberId(Long id, Long memberId);
}