package com.pentakill.ssok.member.infrastructure.persistence;

import com.pentakill.ssok.member.domain.ShippingAddress;
import com.pentakill.ssok.member.domain.ShippingAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ShippingAddressRepositoryImpl
        implements ShippingAddressRepository {

    private final ShippingAddressJpaRepository shippingAddressJpaRepository;

    @Override
    public ShippingAddress saveAndFlush(ShippingAddress shippingAddress) {
        return shippingAddressJpaRepository.saveAndFlush(shippingAddress);
    }

    @Override
    public List<ShippingAddress> findAllByMemberId(Long memberId) {
        return shippingAddressJpaRepository.findAllByMemberId(memberId);
    }

    @Override
    public Optional<ShippingAddress> findByIdAndMemberId(
            Long id,
            Long memberId
    ) {
        return shippingAddressJpaRepository.findByIdAndMemberId(id, memberId);
    }

    @Override
    public void delete(ShippingAddress shippingAddress) {
        shippingAddressJpaRepository.delete(shippingAddress);
    }
}