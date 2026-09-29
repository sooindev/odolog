package com.odolog.app.user.domain.entity;

import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.user.domain.type.Language;
import com.odolog.app.user.domain.type.UnitSystem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private User user() {
        return new User("a@b.com", "encoded", "nick", null);
    }

    @Test
    @DisplayName("새 사용자의 기본 설정은 기존 행의 기본값과 같다")
    void defaultsMatchExistingRows() {
        // @ColumnDefault 와 필드 초기값이 어긋나면 기존·신규 사용자가 서로 다른 설정
        User user = user();

        assertThat(user.getLanguage()).isEqualTo(Language.KO);
        assertThat(user.getTimeZone()).isEqualTo("Asia/Seoul");
        assertThat(user.getCurrency()).isEqualTo("KRW");
        assertThat(user.getUnitSystem()).isEqualTo(UnitSystem.KM_PER_L);
    }

    @Test
    @DisplayName("IANA 지역 이름은 받는다")
    void acceptsRegionTimeZone() {
        User user = user();

        user.changeTimeZone("America/Los_Angeles");

        assertThat(user.getTimeZone()).isEqualTo("America/Los_Angeles");
    }

    @Test
    @DisplayName("고정 오프셋과 없는 이름은 거절한다")
    void rejectsOffsetAndUnknownTimeZone() {
        // 고정 오프셋은 서머타임을 못 따라가 미국·영국에서 1년의 절반이 한 시간 어긋남
        User user = user();

        assertThatThrownBy(() -> user.changeTimeZone("-08:00"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> user.changeTimeZone("Mars/Olympus"))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(user.getTimeZone()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("ISO 4217 통화 코드만 받는다")
    void acceptsOnlyIsoCurrency() {
        User user = user();

        user.changeCurrency("USD");

        assertThat(user.getCurrency()).isEqualTo("USD");
        assertThatThrownBy(() -> user.changeCurrency("XYZ"))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(user.getCurrency()).isEqualTo("USD");
    }
}
