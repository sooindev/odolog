package com.odolog.app.user.repository;

import com.odolog.app.user.domain.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 행 잠금 조회. 한 사용자의 계정 단위 쓰기(가져오기)를 한 줄로 세움 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findLockedById(Long id);

    /** 행 잠금 조회. 같은 주소의 재설정 발급을 한 줄로 세움(토큰 삭제·저장끼리 충돌 방지) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findLockedByEmail(String email);
}
