package com.pentakill.ssok.member.presentation.dto;

import com.pentakill.ssok.member.domain.Member;

public record MemberResponse(Long id, String email) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail());
    }
}
