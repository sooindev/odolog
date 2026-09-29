package com.odolog.app.user.service.time;

import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.repository.jpa.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 사용자 시간대 기준 오늘. 서버 시간대와 무관
 * 미래 날짜 판정·지남 판정·월별 12칸이 공유
 */
@Component
public class UserToday {

    private final UserRepository userRepository;
    private final Clock clock;

    // 스프링이 쓸 생성자 지정. 아래 생성자는 테스트의 시계 주입용
    @Autowired
    public UserToday(UserRepository userRepository) {
        this(userRepository, Clock.systemUTC());
    }

    UserToday(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public LocalDate of(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + userId));

        return LocalDate.now(clock.withZone(ZoneId.of(user.getTimeZone())));
    }
}
