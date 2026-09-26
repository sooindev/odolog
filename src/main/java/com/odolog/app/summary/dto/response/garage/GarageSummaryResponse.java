package com.odolog.app.summary.dto.response.garage;

import com.odolog.app.maintenance.domain.type.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 홈 화면 한 장에 필요한 값 전부
 * 프론트 집계(요청 1 + 차량수 × 2)를 대체 — 페이지 상한도 계산 중복도 없음
 */
public record GarageSummaryResponse(
        int vehicleCount,
        long totalOdometer,

        /** 정비 + 주유 건수 */
        int recordCount,
        /** 정비비 + 유류비. 구성(아래 둘)도 함께 — 합계만으로는 어느 쪽이 큰지 모름 */
        long totalCost,
        long maintenanceCost,
        long fuelCost,

        /** 최근 12개월. 빈 달도 0 으로 채워 12칸 유지 */
        List<MonthlyCost> monthly,
        /** 정비 종류별. 기록 있는 종류만, 비용 내림차순 */
        List<TypeCost> byType,
        List<VehicleLine> vehicles,
        /** 정비·주유를 섞은 최근 5건 */
        List<RecentActivity> recent
) {

    public record MonthlyCost(
            /** 'YYYY-MM' */
            String month,
            long cost,
            long maintenanceCost,
            long fuelCost,
            int count
    ) {
    }

    public record TypeCost(ServiceType type, long cost, int count) {
    }

    public record VehicleLine(
            /** 공개 id. 차량 상세 링크에 쓴다 */
            String id,
            String plateNumber,
            String manufacturer,
            String modelName,
            int odometer,
            int maintenanceCount,
            /** 정비 이력 없으면 null */
            LocalDate lastServiceDate,
            /** 주유 2건 미만이면 null */
            BigDecimal averageEfficiency,
            /**
             * 권장 주기가 지난 정비 종류 수 (2026-09-25)
             * 차량 상세까지 들어가야만 알 수 있던 것을 목록에서 보이게 한다 —
             * 계산은 차량 상세와 같은 NextService 가 하므로 두 화면이 같은 말을 한다
             */
            int overdueServiceCount
    ) {
    }

    /**
     * 정비·주유 공용. kind 에 따라 채워지는 필드가 다름
     * 봉인 인터페이스로 나눠도 JSON 은 평평해지므로 한 모양으로 둠
     */
    public record RecentActivity(
            /** "MAINTENANCE" 또는 "FUEL" */
            String kind,
            /** 공개 id */
            String recordId,
            LocalDate date,
            /** 공개 id */
            String vehicleId,
            String vehicleName,
            /** 주유는 금액을 안 적었으면 null — 0 이면 "0원에 넣었다" 로 읽힌다(합계와 달리 한 건 표시라서) */
            Long cost,
            /** 정비만 */
            ServiceType type,
            /** 주유만 */
            BigDecimal liters
    ) {
    }
}
