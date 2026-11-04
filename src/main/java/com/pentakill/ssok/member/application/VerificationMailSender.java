package com.pentakill.ssok.member.application;

import java.time.LocalDateTime;

public interface VerificationMailSender {

    void sendVerificationCode(
            String email,
            String code,
            LocalDateTime expiresAt
    );
}