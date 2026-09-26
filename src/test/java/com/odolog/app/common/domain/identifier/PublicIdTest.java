package com.odolog.app.common.domain.identifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicIdTest {

    @Test
    @DisplayName("12자 영문·숫자다 — URL 에 그대로 넣을 수 있어야 한다")
    void shape() {
        assertThat(PublicId.generate()).hasSize(PublicId.LENGTH).matches("[0-9A-Za-z]+");
    }

    @Test
    @DisplayName("만 번 만들어도 겹치지 않는다")
    void doesNotRepeat() {
        // 71비트라 실제 충돌 확률은 사실상 0. 겹치면 생성 로직이 고장 난 것
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(PublicId.generate());
        }
        assertThat(seen).hasSize(10_000);
    }
}
