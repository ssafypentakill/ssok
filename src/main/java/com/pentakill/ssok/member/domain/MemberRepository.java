package com.pentakill.ssok.member.domain;

import java.util.Optional;

public interface MemberRepository {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    Optional<Member> findById(Long id);

    Member saveAndFlush(Member member);
}