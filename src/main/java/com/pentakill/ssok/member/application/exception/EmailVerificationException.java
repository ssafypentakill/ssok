package com.pentakill.ssok.member.application.exception;

import lombok.Getter;

@Getter
public class EmailVerificationException extends RuntimeException {

    public enum Reason {
        RESEND_TOO_SOON,
        EMAIL_ALREADY_REGISTERED,
        MAIL_SEND_FAILED
    }

    private final Reason reason;

    public EmailVerificationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public EmailVerificationException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }
}