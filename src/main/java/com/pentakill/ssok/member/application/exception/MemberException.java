package com.pentakill.ssok.member.application.exception;

public class MemberException extends RuntimeException {
    public enum Reason {
        EMAIL_ALREADY_REGISTERED,
        EMAIL_VERIFICATION_REQUIRED,
        INVALID_INPUT,
        INVALID_CREDENTIALS,
        MEMBER_NOT_FOUND
    }

    private final Reason reason;

    public MemberException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
