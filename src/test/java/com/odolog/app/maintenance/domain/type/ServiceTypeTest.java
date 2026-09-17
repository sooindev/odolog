package com.odolog.app.maintenance.domain.type;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 값을 다시 적지 않고 약속만 고정
 * enum 을 베끼면 값을 고칠 때 테스트도 같이 고치게 되어 아무것도 못 잡음
 */
class ServiceTypeTest {

    @Test
    @DisplayName("OTHER 를 뺀 모든 종류는 주기가 최소 하나는 있다")
    void everyTypeExceptOtherHasAnInterval() {
        // 주기를 빠뜨리면 다음 정비 시점이 영영 안 뜸. 계산이 조용히 null 을 주므로 에러도 없음
        assertThat(Arrays.stream(ServiceType.values())
                .filter(type -> type != ServiceType.OTHER)
                .filter(type -> type.getRecommendedIntervalKm() == null
                        && type.getRecommendedIntervalMonths() == null))
                .isEmpty();
    }

    @Test
    @DisplayName("OTHER 는 주기가 둘 다 없다 — 무엇인지 모르는 정비라 계산할 근거가 없다")
    void otherHasNoInterval() {
        assertThat(ServiceType.OTHER.getRecommendedIntervalKm()).isNull();
        assertThat(ServiceType.OTHER.getRecommendedIntervalMonths()).isNull();
    }

    @Test
    @DisplayName("주기는 0이나 음수가 될 수 없다")
    void intervalsArePositive() {
        for (ServiceType type : ServiceType.values()) {
            if (type.getRecommendedIntervalKm() != null) {
                assertThat(type.getRecommendedIntervalKm())
                        .as("%s 의 km 주기", type).isPositive();
            }
            if (type.getRecommendedIntervalMonths() != null) {
                assertThat(type.getRecommendedIntervalMonths())
                        .as("%s 의 개월 주기", type).isPositive();
            }
        }
    }

    @Test
    @DisplayName("이름이 30자를 넘지 않는다 — type 컬럼이 varchar(30) 이다")
    void nameFitsInColumn() {
        for (ServiceType type : ServiceType.values()) {
            assertThat(type.name().length())
                    .as("%s 의 이름 길이", type).isLessThanOrEqualTo(30);
        }
    }
}
