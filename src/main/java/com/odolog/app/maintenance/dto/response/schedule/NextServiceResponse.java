package com.odolog.app.maintenance.dto.response.schedule;

import com.odolog.app.maintenance.domain.calculation.NextService;
import com.odolog.app.maintenance.domain.type.ServiceType;

import java.time.LocalDate;

/**
 * 종류 하나의 다음 정비 시점
 *
 * @param overdue 주행거리·날짜 중 하나라도 지났는가. 서버가 판정하는 이유 —
 *                화면이 직접 오늘과 비교하면 차량 상세와 홈이 다른 말을 하게 된다
 */
public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate,
        boolean overdue,
        /** 실제로 적용된 주기. 차량별 설정이 있으면 그 값 */
        Integer intervalKm,
        Integer intervalMonths,
        /** 기본값을 덮어쓴 상태인가 */
        boolean customized
) {

    public static NextServiceResponse from(NextService next) {
        return new NextServiceResponse(
                next.type(),
                next.lastOdometer(),
                next.nextOdometer(),
                next.lastDate(),
                next.nextDate(),
                next.overdue(),
                next.intervalKm(),
                next.intervalMonths(),
                next.customized());
    }
}
