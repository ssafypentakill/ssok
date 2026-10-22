package com.pentakill.ssok.member.domain;

import java.util.Optional;

/**
 * 회원 저장·조회 기능을 정의하는 인터페이스다.
 * 실제 DB 접근은 infrastructure의 MemberRepositoryImpl이 구현한다.
 */
public interface MemberRepository {

  /**
 * 정규화한 이메일로 가입된 회원이 있는지 확인한다.
 * 동시 중복 가입은 이 조회만으로 막을 수 없어 DB UNIQUE 제약도 필요하다.
 */
    boolean existsByEmail(String email);

/**
 * 회원을 저장하고 대기 중인 변경 내용을 DB에 flush한다.
 * flush는 커밋이 아니며, 이후 트랜잭션 실패 시 롤백될 수 있다.
 */
    Member saveAndFlush(Member member);

/**
 * 정규화한 이메일로 회원을 조회하며, 없으면 Optional.empty()를 반환한다.
 * 로그인 시 사용하고 비밀번호·회원 상태 확인은 서비스에서 처리한다.
 */
    Optional<Member> findByEmail(String email);

/**
 * 내부 회원 ID로 회원을 조회하며, 없으면 Optional.empty()를 반환한다.
 * 내 정보 조회에는 세션의 인증정보에서 확인한 회원 ID를 사용한다.
 */
    Optional<Member> findById(Long id);
}