package com.odolog.app.user;

import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTodayTest {

    // 서울 09-29 21:00 = 오클랜드(서머타임 UTC+13) 09-30 01:00 = 로스앤젤레스 09-29 05:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC);

    private UserToday userTodayIn(String timeZone) {
        User user = new User("a@b.com", "encoded", "nick");
        user.changeTimeZone(timeZone);

        UserRepository repository = mock(UserRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        return new UserToday(repository, CLOCK);
    }

    private LocalDate todayIn(String timeZone) {
        return userTodayIn(timeZone).of(1L);
    }

    @Test
    @DisplayName("같은 순간이라도 사용자 시간대에 따라 오늘이 다르다")
    void todayFollowsUserTimeZone() {
        assertThat(todayIn("Asia/Seoul")).isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(todayIn("Pacific/Auckland")).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(todayIn("America/Los_Angeles")).isEqualTo(LocalDate.of(2026, 9, 29));
    }

    @Test
    @DisplayName("서버 시간대가 아니라 사용자 시간대 — 서울로는 아직 29일")
    void ignoresServerTimeZone() {
        // 서울 고정이었을 때 오클랜드 사용자의 오늘(30일) 기록이 미래로 판정
        assertThat(LocalDate.now(CLOCK.withZone(ZoneId.of("Asia/Seoul"))))
                .isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(todayIn("Pacific/Auckland")).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    @DisplayName("오클랜드의 오늘(30일)은 서울이 아직 29일이어도 저장된다")
    void acceptsTodayAheadOfSeoul() {
        UserToday auckland = userTodayIn("Pacific/Auckland");

        assertThatCode(() -> auckland.rejectFuture(1L, LocalDate.of(2026, 9, 30), "serviceDate"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("사용자의 내일은 400, 안 보낸 날짜(null)는 통과")
    void rejectsTomorrowButNotNull() {
        UserToday seoul = userTodayIn("Asia/Seoul");

        assertThatThrownBy(() -> seoul.rejectFuture(1L, LocalDate.of(2026, 9, 30), "serviceDate"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageStartingWith("serviceDate: ");
        assertThatCode(() -> seoul.rejectFuture(1L, null, "serviceDate"))
                .doesNotThrowAnyException();
    }
}
