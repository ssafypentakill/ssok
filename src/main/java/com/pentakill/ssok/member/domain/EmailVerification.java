package com.pentakill.ssok.member.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "email_verification",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_email_verification_email",
            columnNames = "email"
        )
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailVerification {

    private static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_id", nullable = false)
    private UUID verificationId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    // 재전송 시 기존 인증번호를 교체합니다.
    public void reissue(String codeHash, Instant expiresAt) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = null;
        this.attemptCount = 0;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isAttemptLimitReached() {
        return attemptCount >= MAX_ATTEMPTS;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void recordFailure() {
        if (isAttemptLimitReached()) {
            throw new IllegalStateException(
                "인증번호 입력 횟수를 초과했습니다."
            );
        }

        attemptCount++;
    }

    public void verify(Instant now) {
        if (isExpired(now) || isAttemptLimitReached()) {
            throw new IllegalStateException(
                "인증번호를 재전송해주세요."
            );
        }

        verifiedAt = now;
    }

    public boolean canSignup(Instant now) {
        return isVerified()
            && !isExpired(now)
            && !isAttemptLimitReached();
    }
}