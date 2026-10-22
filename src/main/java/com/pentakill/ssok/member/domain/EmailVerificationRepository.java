package com.pentakill.ssok.member.domain;

import java.util.Optional;

/**
 * 이메일 인증 기록의 조회·저장·삭제 기능을 정의한다.
 * 실제 DB 접근과 잠금 설정은 infrastructure에서 구현한다.
 */
public interface EmailVerificationRepository {
  
/**
 * 인증 확인·재전송·가입 시 인증 기록을 잠금 조회하며, 없으면 Optional.empty()를 반환한다.
 * 트랜잭션 안에서 사용하고 구현체에 PESSIMISTIC_WRITE 설정이 필요하다.
 * 존재하지 않는 행은 잠기지 않으므로 최초 발급의 UNIQUE 충돌은 별도 처리한다.
 */
    Optional<EmailVerification> findByEmailForUpdate(String email);

/**
 * 인증번호 원문 대신 검증값을 담은 인증 기록을 저장하고 flush한다.
 * 실패 횟수 증가 후 트랜잭션이 롤백되면 횟수도 되돌아가므로 주의한다.
 */
    EmailVerification saveAndFlush(EmailVerification verification);

/**
 * 회원가입에 사용한 인증 기록을 삭제한다.
 * 회원·배송지 저장과 같은 트랜잭션에서 처리하여 함께 성공하거나 롤백되게 한다.
 */
    void delete(EmailVerification verification);
}
