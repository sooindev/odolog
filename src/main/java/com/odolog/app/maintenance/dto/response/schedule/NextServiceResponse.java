package com.odolog.app.maintenance.dto.response.schedule;

import com.odolog.app.maintenance.domain.calculation.NextService;
import com.odolog.app.maintenance.domain.type.ServiceType;

import java.time.LocalDate;

/**
 * 종류 하나의 다음 정비 시점
 *
 * @param overdue 주행거리·날짜 중 하나라도 지남. 화면 간 판정 일치를 위해 서버 판정
 */
public record NextServiceResponse(
        ServiceType type,
        Integer lastServiceOdometer,
        Integer nextServiceOdometer,
        LocalDate lastServiceDate,
        LocalDate nextServiceDate,
        boolean overdue,
        /** 실제 적용 주기. 차량별 설정 우선 */
        Integer intervalKm,
        Integer intervalMonths,
        /** 기본값 덮어씀 여부 */
        boolean customized,
        /** 직접 정한 km 주기. 기본값이면 null. 편집 폼이 기본값을 설정으로 굳히지 않게 */
        Integer customIntervalKm,
        /** 직접 정한 개월 주기. 기본값이면 null */
        Integer customIntervalMonths
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
                next.customized(),
                next.customIntervalKm(),
                next.customIntervalMonths());
    }
}
