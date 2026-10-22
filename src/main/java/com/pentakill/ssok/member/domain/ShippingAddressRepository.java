package com.pentakill.ssok.member.domain;

import java.util.List;
import java.util.Optional;

/**
 * 회원별 배송지 저장·조회·삭제 기능을 정의한다.
 * 실제 DB 접근은 infrastructure의 ShippingAddressRepositoryImpl이 구현한다.
 */
public interface ShippingAddressRepository {
/**
 * 회원과 연결된 배송지를 저장하고 flush한다.
 * 가입 시 회원 저장과 같은 트랜잭션에서 처리하며, flush는 커밋이 아니다.
 */
    ShippingAddress saveAndFlush(ShippingAddress shippingAddress);
/**
 * 해당 회원의 배송지를 모두 조회하며, 없으면 빈 목록을 반환한다.
 * 회원 ID는 로그인 인증정보에서 얻고, 조회 순서는 별도로 지정해야 한다.
 */
    List<ShippingAddress> findAllByMemberId(Long memberId);

/**
 * 배송지 ID와 로그인한 회원 ID를 함께 조건으로 조회한다.
 * 없거나 다른 회원 소유라면 Optional.empty()를 반환하며 수정·삭제 전 확인에 사용한다.
 */
    Optional<ShippingAddress> findByIdAndMemberId(Long id, Long memberId);

/**
 * 서비스에서 소유자를 확인한 배송지를 삭제한다.
 * 회원은 삭제하지 않으며, 트랜잭션이 롤백되면 배송지 삭제도 취소된다.
 */
    void delete(ShippingAddress shippingAddress);
}
