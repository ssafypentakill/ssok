package com.pentakill.ssok.member.application;

import com.pentakill.ssok.member.application.exception.MemberException;
import com.pentakill.ssok.member.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.pentakill.ssok.member.application.exception.MemberException.Reason.*;

@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ShippingAddressRepository shippingAddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public MemberService(MemberRepository memberRepository,
                         EmailVerificationRepository emailVerificationRepository,
                         ShippingAddressRepository shippingAddressRepository,
                         PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.shippingAddressRepository = shippingAddressRepository;
        this.passwordEncoder = passwordEncoder;
        // 없는 이메일에도 해시 비교를 수행해 즉시 실패하는 경로를 줄인다.
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Member signup(String email, String password, String recipientName,
                         String recipientPhone, String postalCode,
                         String address, String addressDetail) {
        String normalizedEmail = normalizeEmail(email);
        if (password == null || password.isBlank() || password.length() < 8) {
            throw new MemberException(INVALID_INPUT);
        }
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new MemberException(EMAIL_ALREADY_REGISTERED);
        }

        EmailVerification verification = emailVerificationRepository
                .findByEmailForUpdate(normalizedEmail)
                .orElseThrow(() -> new MemberException(EMAIL_VERIFICATION_REQUIRED));
        // 잠금 대기 시간을 반영하기 위해 조회가 끝난 다음 시각을 확인한다.
        if (!verification.canSignup(LocalDateTime.now())) {
            throw new MemberException(EMAIL_VERIFICATION_REQUIRED);
        }

        Member member = memberRepository.saveAndFlush(Member.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .build());

        shippingAddressRepository.saveAndFlush(ShippingAddress.builder()
                .member(member)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .postalCode(postalCode)
                .address(address)
                .addressDetail(addressDetail)
                .build());

        emailVerificationRepository.delete(verification);
        return member;
    }

    public Member authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (password == null || password.isBlank()) {
            throw new MemberException(INVALID_CREDENTIALS);
        }
        Member member = memberRepository.findByEmail(normalizedEmail).orElse(null);
        String hash = member != null && member.getPasswordHash() != null
                ? member.getPasswordHash() : dummyPasswordHash;
        boolean passwordMatches = passwordEncoder.matches(password, hash);
        if (member == null || !member.isLocal() || !member.isActive() || !passwordMatches) {
            throw new MemberException(INVALID_CREDENTIALS);
        }
        return member;
    }

    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 254) {
            throw new MemberException(INVALID_INPUT);
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
