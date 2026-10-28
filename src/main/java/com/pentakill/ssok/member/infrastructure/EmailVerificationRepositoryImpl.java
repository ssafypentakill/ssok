package com.pentakill.ssok.member.infrastructure.persistence;

import com.pentakill.ssok.member.domain.EmailVerification;
import com.pentakill.ssok.member.domain.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepositoryImpl
        implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository emailVerificationJpaRepository;

    @Override
    public Optional<EmailVerification> findByEmailForUpdate(String email) {
        return emailVerificationJpaRepository.findByEmailForUpdate(email);
    }

    @Override
    public EmailVerification saveAndFlush(EmailVerification verification) {
        return emailVerificationJpaRepository.saveAndFlush(verification);
    }

    @Override
    public void delete(EmailVerification verification) {
        emailVerificationJpaRepository.delete(verification);
    }
}