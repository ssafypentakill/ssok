package com.pentakill.ssok.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "shipping_address", indexes = @Index(name = "idx_shipping_address_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private ShippingAddress(Member member, String recipientName, String recipientPhone,
                            String postalCode, String address, String addressDetail) {
        this.member = Objects.requireNonNull(member, "member");
        update(recipientName, recipientPhone, postalCode, address, addressDetail);
    }

    public void update(String recipientName, String recipientPhone, String postalCode,
                       String address, String addressDetail) {
        requireText(recipientName, 100);
        requireText(recipientPhone, 30);
        requireText(postalCode, 10);
        requireText(address, 255);
        if (addressDetail != null && addressDetail.length() > 255) {
            throw new IllegalArgumentException("Address detail too long");
        }
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

    private static void requireText(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException("Invalid shipping address field");
        }
    }

    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() { updatedAt = Instant.now(); }
}
