package com.odolog.app.common.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("짝 없는 서로게이트만 '?' 로 바꾸고 짝이 맞는 이모지는 그대로 둔다")
    void optionalReplacesUnpairedSurrogates() {
        assertThat(InputText.optional("a\uD83Db")).isEqualTo("a?b");
        assertThat(InputText.optional("세차 🚗")).isEqualTo("세차 🚗");
    }
}
