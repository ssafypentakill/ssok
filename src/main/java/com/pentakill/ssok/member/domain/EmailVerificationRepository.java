package com.pentakill.ssok.member.domain;

import java.util.Optional;

public interface EmailVerificationRepository {

    Optional<EmailVerification> findByEmailForUpdate(String email);

    EmailVerification saveAndFlush(
        EmailVerification emailVerification
    );

    void delete(EmailVerification emailVerification);
}