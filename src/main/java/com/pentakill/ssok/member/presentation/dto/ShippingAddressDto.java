package com.pentakill.ssok.member.presentation.dto;

import com.pentakill.ssok.member.domain.ShippingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class ShippingAddressDto {
    private ShippingAddressDto() {}

    @Getter
    @NoArgsConstructor
    public static class Request {
        @NotBlank @Size(max = 100)
        private String recipientName;

        @NotBlank @Size(max = 30)
        private String recipientPhone;

        @NotBlank @Size(max = 10)
        private String postalCode;

        @NotBlank @Size(max = 255)
        private String address;

        @Size(max = 255)
        private String addressDetail;
    }

    public record Response(Long id, String recipientName, String recipientPhone,
                           String postalCode, String address, String addressDetail) {
        public static Response from(ShippingAddress address) {
            return new Response(address.getId(), address.getRecipientName(),
                    address.getRecipientPhone(), address.getPostalCode(),
                    address.getAddress(), address.getAddressDetail());
        }
    }
}
