package com.odolog.app.summary.dto.response.garage;

import com.odolog.app.maintenance.domain.type.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 홈 화면 한 장에 필요한 모든 값.
 *
 * <p><b>왜 한 덩어리인가.</b> 전에는 프론트가 차량 목록 1번 + 차량마다 정비·주유 목록을 받아
 * 직접 더했다(요청 1 + 차량수 × 2). 그러면 두 가지가 따라온다:
 * 페이지 상한(200건)을 넘는 기록은 합계에서 빠지고, 같은 계산이 서버·클라이언트 두 곳에 생긴다.
 * 화면에 "일부 정비 기록만 합산됨"이라는 단서를 달고 있던 게 그 대가다.
 *
 * <p>서버는 SQL 로 전부 읽어 더하므로 상한이 없고, 요청도 1번이다.
 */
public record GarageSummaryResponse(
        int vehicleCount,
        long totalOdometer,

        /** 정비 + 주유 건수. */
        int recordCount,
        /** 정비비 + 유류비. 구성을 알아야 어느 쪽이 큰지 보이므로 아래 둘도 함께 준다. */
        long totalCost,
        long maintenanceCost,
        long fuelCost,

        /** 최근 12개월. 기록이 없는 달도 0 으로 채워 12칸을 유지한다. */
        List<MonthlyCost> monthly,
        /** 정비 종류별. 기록이 있는 종류만, 비용 내림차순. */
        List<TypeCost> byType,
        List<VehicleLine> vehicles,
        /** 정비·주유를 한데 섞은 최근 5건. */
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
            Long id,
            String plateNumber,
            String manufacturer,
            String modelName,
            int odometer,
            int maintenanceCount,
            /** 정비 이력이 없으면 null. */
            LocalDate lastServiceDate,
            /** 주유 기록이 2건 미만이면 null. */
            BigDecimal averageEfficiency
    ) {
    }

    /**
     * 정비와 주유가 한 목록에 섞인다.
     *
     * <p>kind 에 따라 채워지는 필드가 다르다 — 정비면 type/cost, 주유면 liters/cost.
     * 자바에서 봉인 인터페이스로 나눌 수도 있지만, JSON 으로 나가면 어차피 평평해지고
     * 클라이언트는 kind 로 갈라 읽는다. 한 모양으로 두는 편이 양쪽 다 단순하다.
     */
    public record RecentActivity(
            /** "MAINTENANCE" 또는 "FUEL" */
            String kind,
            Long recordId,
            LocalDate date,
            Long vehicleId,
            String vehicleName,
            long cost,
            /** 정비일 때만. */
            ServiceType type,
            /** 주유일 때만. */
            BigDecimal liters
    ) {
    }
}
