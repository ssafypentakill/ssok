package com.pentakill.ssok.member.application;

import com.pentakill.ssok.member.domain.EmailVerification;
import com.pentakill.ssok.member.domain.EmailVerificationRepository;
import com.pentakill.ssok.member.domain.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
@Transactional
public class EmailVerificationService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int CODE_BOUND = 1_000_000;
    private static final int SALT_BYTES = 16;
    private static final int MIN_SECRET_BYTES = 32;
    private static final long EXPIRATION_MINUTES = 5;

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository verificationRepository;
    private final VerificationMailSender mailSender;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec hmacKey;

    public EmailVerificationService(
            MemberRepository memberRepository,
            EmailVerificationRepository verificationRepository,
            VerificationMailSender mailSender,
            @Value("${member.email-verification.hmac-secret}")
            String encodedSecret
    ) {
        this.memberRepository = memberRepository;
        this.verificationRepository = verificationRepository;
        this.mailSender = mailSender;
        this.hmacKey = createHmacKey(encodedSecret);
    }

    public void sendCode(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다");
        }

        EmailVerification verification = verificationRepository
                .findByEmailForUpdate(normalizedEmail)
                .orElse(null);

        String code = generateCode();
        String codeHash = createCodeHash(normalizedEmail, code);

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(EXPIRATION_MINUTES);

        if (verification == null) {
            verification = EmailVerification.builder()
                    .email(normalizedEmail)
                    .codeHash(codeHash)
                    .expiresAt(expiresAt)
                    .build();
        } else {
            verification.reissue(codeHash, expiresAt);
        }

        verificationRepository.saveAndFlush(verification);

        sendMailAfterCommit(normalizedEmail, code, expiresAt);
    }

    public boolean confirmCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);

        EmailVerification verification = verificationRepository
                .findByEmailForUpdate(normalizedEmail)
                .orElse(null);

        if (verification == null) {
            return false;
        }

        // 잠금 획득 후 현재 시각을 확인한다.
        LocalDateTime now = LocalDateTime.now();

        if (verification.isExpired(now)
                || verification.isAttemptLimitReached()
                || verification.isVerified()) {
            return false;
        }

        if (!isSixDigitCode(code)
                || !matches(normalizedEmail, code, verification.getCodeHash())) {

            verification.increaseAttemptCount(now);
            verificationRepository.saveAndFlush(verification);
            return false;
        }

        verification.markVerified(now);
        verificationRepository.saveAndFlush(verification);

        return true;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 254) {
            throw new IllegalArgumentException("올바른 이메일을 입력해 주세요");
        }

        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        return String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(CODE_BOUND)
        );
    }

    private boolean isSixDigitCode(String code) {
        return code != null && code.matches("[0-9]{6}");
    }

    private String createCodeHash(String email, String code) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);

        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        byte[] digest = calculateHmac(email, encodedSalt, code);
        String encodedDigest = Base64.getEncoder().encodeToString(digest);

        return encodedSalt + ":" + encodedDigest;
    }

    private boolean matches(String email, String code, String storedHash) {
        String[] parts = storedHash.split(":", -1);

        if (parts.length != 2) {
            throw new IllegalStateException("인증 검증값 형식이 올바르지 않습니다");
        }

        byte[] expectedDigest;

        try {
            expectedDigest = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "인증 검증값 형식이 올바르지 않습니다",
                    exception
            );
        }

        byte[] actualDigest = calculateHmac(email, parts[0], code);

        return MessageDigest.isEqual(expectedDigest, actualDigest);
    }

    private byte[] calculateHmac(
            String email,
            String encodedSalt,
            String code
    ) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);

            // 이메일도 검증값에 포함해 다른 이메일의 기록과 구분한다.
            String input = email + "\n" + encodedSalt + "\n" + code;

            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "이메일 인증 검증값을 생성할 수 없습니다",
                    exception
            );
        }
    }

    private SecretKeySpec createHmacKey(String encodedSecret) {
        byte[] secret;

        try {
            secret = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "이메일 인증 비밀키는 Base64 형식이어야 합니다"
            );
        }

        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "이메일 인증 비밀키는 최소 32바이트여야 합니다"
            );
        }

        return new SecretKeySpec(secret, HMAC_ALGORITHM);
    }

    private void sendMailAfterCommit(
            String email,
            String code,
            LocalDateTime expiresAt
    ) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("활성 트랜잭션이 필요합니다");
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        mailSender.sendVerificationCode(
                                email,
                                code,
                                expiresAt
                        );
                    }
                }
        );
    }
}