package com.pentakill.ssok.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification", uniqueConstraints = @UniqueConstraint(name = "uk_email_verification_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {
    public static final int MAX_ATTEMPTS = 5;

    @Id
    @Column(name = "verification_id", nullable = false, updatable = false)
    private UUID verificationId;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Builder
    private EmailVerification(String email, String codeHash, Instant expiresAt) {
        if (email == null || email.isBlank() || email.length() > 254) {
            throw new IllegalArgumentException("Invalid email");
        }
        this.verificationId = UUID.randomUUID();
        this.email = email.strip().toLowerCase(java.util.Locale.ROOT);
        reissue(codeHash, expiresAt);
    }

    public boolean isExpired(Instant now) { return !now.isBefore(expiresAt); }
    public boolean isAttemptLimitReached() { return attemptCount >= MAX_ATTEMPTS; }
    public boolean isVerified() { return verifiedAt != null; }

    public void reissue(String codeHash, Instant expiresAt) {
        if (codeHash == null || codeHash.isBlank() || codeHash.length() > 255 || expiresAt == null) {
            throw new IllegalArgumentException("Invalid verification data");
        }
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.verifiedAt = null;
        this.attemptCount = 0;
    }

    public void increaseAttemptCount(Instant now) {
        requireUsable(now);
        attemptCount++;
    }

    public void markVerified(Instant now) {
        requireUsable(now);
        verifiedAt = now;
    }

    public boolean canSignup(Instant now) {
        return isVerified() && !isExpired(now) && !isAttemptLimitReached();
    }

    private void requireUsable(Instant now) {
        if (isExpired(now) || isAttemptLimitReached() || isVerified()) {
            throw new IllegalStateException("Verification is not usable");
        }
    }
}