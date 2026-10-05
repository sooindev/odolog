package com.odolog.app.user;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.user.domain.User;
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

        return of(user);
    }

    /** 이미 읽은 사용자로. 같은 요청에서 조회 중복 방지 */
    public LocalDate of(User user) {
        return LocalDate.now(clock.withZone(ZoneId.of(user.getTimeZone())));
    }

    /** 사용자의 오늘 이후면 400. null 은 통과(안 보낸 값). @PastOrPresent 는 서버 시간대 기준이라 미사용 */
    public void rejectFuture(Long userId, LocalDate date, String field) {
        if (date != null) {
            rejectFuture(of(userId), date, field);
        }
    }

    /** 이미 계산한 오늘 기준. 여러 날짜를 한 번에 검사할 때(가져오기) */
    public static void rejectFuture(LocalDate today, LocalDate date, String field) {
        if (date != null && date.isAfter(today)) {
            throw new InvalidRequestException(ErrorCode.FUTURE_DATE,
                    field + ": 오늘 이후 날짜는 입력할 수 없습니다.", field);
        }
    }
}
