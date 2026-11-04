package com.pentakill.ssok.member.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

public final class MemberRequest {
    private MemberRequest() {}

    @Getter
    @NoArgsConstructor
    public static class Signup {
        @NotBlank @Email @Size(max = 254)
        private String email;

        @NotBlank
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다")
        private String password;

        @NotNull(message = "배송지를 입력해 주세요")
        @Valid
        private ShippingAddressDto.Request shippingAddress;
    }

    @Getter
    @NoArgsConstructor
    public static class Login {
        @NotBlank @Email @Size(max = 254)
        private String email;

        @NotBlank
        private String password;
    }
}
