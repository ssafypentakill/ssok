package com.pentakill.ssok.member.presentation;

import com.pentakill.ssok.member.application.EmailVerificationService;
import com.pentakill.ssok.member.presentation.dto.EmailVerificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/email-verifications")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/send")
    public ResponseEntity<Void> sendCode(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        emailVerificationService.sendCode(request.getEmail());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmCode(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        String code = request.getCode();

        if (code == null || !code.matches("[0-9]{6}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "인증번호는 6자리 숫자여야 합니다"
            );
        }

        boolean verified = emailVerificationService.confirmCode(
                request.getEmail(),
                code
        );

        if (!verified) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이메일 인증에 실패했습니다"
            );
        }

        return ResponseEntity.noContent().build();
    }
}