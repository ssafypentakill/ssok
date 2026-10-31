package com.pentakill.ssok.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"", uniqueConstraints = @UniqueConstraint(name = "uk_user_email", columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Current scope: local registration only. Do not accept provider/status from the client.
    @Builder
    private Member(String email, String passwordHash) {
        if (email == null || email.isBlank() || email.length() > 254) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > 255) {
            throw new IllegalArgumentException("Invalid password hash");
        }
        this.email = email.strip().toLowerCase(java.util.Locale.ROOT);
        this.passwordHash = passwordHash;
        this.provider = "LOCAL";
        this.status = "ACTIVE";
    }

    public boolean isActive() { return "ACTIVE".equals(status); }
    public boolean isLocal() { return "LOCAL".equals(provider); }

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}