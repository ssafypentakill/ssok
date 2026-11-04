package com.pentakill.ssok.member.infrastructure.mail;

import com.pentakill.ssok.member.application.VerificationMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SmtpVerificationMailSender implements VerificationMailSender {

    private static final DateTimeFormatter EXPIRATION_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpVerificationMailSender(
            JavaMailSender mailSender,
            @Value("${member.email-verification.from-address}")
            String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(
            String email,
            String code,
            LocalDateTime expiresAt
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("[SSOK] 회원가입 이메일 인증번호");
        message.setText("""
                안녕하세요. SSOK입니다.

                회원가입 이메일 인증번호를 안내드립니다.

                인증번호: %s

                만료 시각: %s
                발급 후 5분 안에 이메일 인증과 회원가입을 완료해 주세요.

                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(
                code,
                expiresAt.format(EXPIRATION_FORMAT)
        ));

        mailSender.send(message);
    }
}