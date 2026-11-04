package com.pentakill.ssok.member.presentation;

import com.pentakill.ssok.member.application.exception.MemberException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = MemberController.class)
public class MemberExceptionHandler {
    @ExceptionHandler(MemberException.class)
    public ResponseEntity<ProblemDetail> handleMember(MemberException exception) {
        return switch (exception.getReason()) {
            case EMAIL_ALREADY_REGISTERED -> problem(HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다");
            case EMAIL_VERIFICATION_REQUIRED -> problem(HttpStatus.BAD_REQUEST,
                    "EMAIL_VERIFICATION_REQUIRED", "유효한 이메일 인증을 완료해 주세요");
            case INVALID_INPUT -> problem(HttpStatus.BAD_REQUEST,
                    "INVALID_INPUT", "이메일과 8자 이상의 비밀번호를 확인해 주세요");
            case INVALID_CREDENTIALS -> problem(HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다");
            case MEMBER_NOT_FOUND -> problem(HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다");
        };
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        // rejectedValue를 응답하지 않아 비밀번호 등의 입력값 노출을 방지한다.
        var response = problem(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력값을 확인해 주세요");
        response.getBody().setProperty("errors", exception.getBindingResult()
                .getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList());
        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_JSON", "요청 본문을 확인해 주세요");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_user_email".equals(violation.getConstraintName())) {
                return problem(HttpStatus.CONFLICT,
                        "EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다");
            }
        }
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "MEMBER_STORAGE_FAILED", "회원 정보를 저장하는 중 오류가 발생했습니다");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String message) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, message);
        body.setProperty("code", code);
        return ResponseEntity.status(status).body(body);
    }

    private record FieldError(String field, String message) {}
}
