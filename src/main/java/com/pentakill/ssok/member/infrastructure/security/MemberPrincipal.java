package com.pentakill.ssok.member.infrastructure.security;

import java.io.Serializable;

/** 세션에는 엔티티나 비밀번호 대신 필요한 회원 식별 정보만 저장한다. */
public record MemberPrincipal(Long memberId, String email) implements Serializable {
}
