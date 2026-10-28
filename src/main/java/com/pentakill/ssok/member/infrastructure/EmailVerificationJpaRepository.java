package com.pentakill.ssok.member.infrastructure.persistence;

import com.pentakill.ssok.member.domain.EmailVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationJpaRepository
        extends JpaRepository<EmailVerification, UUID> {

    /**
     * 인증 기록을 조회하면서 쓰기 잠금을 획득한다.
     * 서비스의 트랜잭션 안에서 호출해야 한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select v
            from EmailVerification v
            where v.email = :email
            """)
    Optional<EmailVerification> findByEmailForUpdate(
            @Param("email") String email
    );
}