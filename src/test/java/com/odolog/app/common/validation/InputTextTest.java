package com.odolog.app.common.validation;

import com.odolog.app.common.exception.type.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputTextTest {

    @Test
    @DisplayName("선택 입력은 앞뒤 공백을 지우고, 비면 null 로 바꾼다 — 전각 공백까지")
    void optionalStripsAndBlanksToNull() {
        assertThat(InputText.optional(null)).isNull();
        assertThat(InputText.optional("   ")).isNull();
        assertThat(InputText.optional("　")).isNull();
        assertThat(InputText.optional("  엔진오일 교체 ")).isEqualTo("엔진오일 교체");
    }

    @Test
    @DisplayName("strip() 이 남기는 NBSP·폭 없는 공백·BOM 도 지운다 — 화면에 빈 칸으로 보이는 닉네임 방지")
    void stripsInvisibleSpaces() {
        assertThat(InputText.optional("\u00a0")).isNull();
        assertThat(InputText.optional("\u200b\ufeff")).isNull();
        assertThat(InputText.optional("\u202f메모\u2007")).isEqualTo("메모");
        // 가운데 NBSP 는 글자 사이라 남김
        assertThat(InputText.optional("a\u00a0b")).isEqualTo("a\u00a0b");
        assertThatThrownBy(() -> InputText.required("\u00a0", "nickname"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("짝 없는 서로게이트만 '?' 로 바꾸고 짝이 맞는 이모지는 그대로 둔다")
    void optionalReplacesUnpairedSurrogates() {
        assertThat(InputText.optional("a\uD83Db")).isEqualTo("a?b");
        assertThat(InputText.optional("세차 🚗")).isEqualTo("세차 🚗");
    }
}
