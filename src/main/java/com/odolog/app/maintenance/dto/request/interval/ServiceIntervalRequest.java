package com.odolog.app.maintenance.dto.request.interval;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/**
 * 이 차량에서 쓸 권장 주기
 *
 * 부분 수정이 아니라 <전체 교체>다 — 둘 다 null 이면 기본값으로 되돌린다.
 * "안 보냄" 과 "비움" 을 가를 필요가 없는 이유: 이 요청의 뜻이 "이 종류의 주기를 이렇게 하라"
 * 하나뿐이고, 화면이 두 칸을 언제나 함께 보낸다
 */
public record ServiceIntervalRequest(

        /** 주행거리 주기(km). null 이면 기본값 */
        @Positive
        @Max(500_000)
        Integer intervalKm,

        /** 기간 주기(개월). null 이면 기본값. 10년이면 사실상 "안 봄" 이라 상한을 거기 둔다 */
        @Positive
        @Max(120)
        Integer intervalMonths
) {
}
