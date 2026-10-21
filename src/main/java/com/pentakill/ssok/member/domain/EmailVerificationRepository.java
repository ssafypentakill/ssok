package com.pentakill.ssok.member.domain;

import java.util.Optional;

public interface EmailVerificationRepository {

    Optional<EmailVerification> findByEmailForUpdate(String email);

    EmailVerification saveAndFlush(
        EmailVerification verification
    );

    void delete(EmailVerification verification);
}