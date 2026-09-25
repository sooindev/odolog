package com.odolog.app.maintenance.domain.calculation;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 다음 정비 시점과 "지났는가" 판정
 *
 * 엔티티가 아니라 값 계산이라 entity 와 형제로 뒀다 (fuel/domain/calculation 과 같은 자리)
 *
 * 차량 상세와 홈 요약이 이것을 공유한다. 두 벌이면 한 화면은 지났다 하고 다른 화면은
 * 아무 말도 안 하게 된다 — 연비 공식을 하나로 합친 것과 같은 이유다
 *
 * @param overdue 주행거리와 날짜 중 하나라도 지났는가.
 *                권장 주기가 "km 또는 개월 중 먼저 오는 것" 이므로 둘 중 하나면 충분하다
 */
public record NextService(
        ServiceType type,
        int lastOdometer,
        Integer nextOdometer,
        LocalDate lastDate,
        LocalDate nextDate,
        boolean overdue
) {

    /**
     * 종류별 다음 정비 시점. 이력 있는 종류만, 지난 것이 먼저
     *
     * 지난 것을 위로 올리는 이유: 이 목록은 "뭘 해야 하나" 를 보는 자리다.
     * 순서를 서버가 정하는 것은 그대로다 — 화면마다 정렬이 달라지면 같은 차가 다르게 보인다
     *
     * @param records         한 차량의 정비 이력 (정렬 무관 — 종류별 최신을 여기서 고른다)
     * @param currentOdometer 그 차량의 현재 주행거리
     * @param today           "오늘". 밖에서 받는다 — 안에서 now() 를 부르면 테스트에서 못 고정한다
     */
    public static List<NextService> of(List<MaintenanceRecord> records, int currentOdometer,
                                       LocalDate today) {

        Map<ServiceType, MaintenanceRecord> latest = new EnumMap<>(ServiceType.class);
        for (MaintenanceRecord record : records) {
            latest.merge(record.getType(), record, NextService::newerOf);
        }

        List<NextService> results = new ArrayList<>(latest.size());
        // enum 선언 순서를 먼저 깔고, 그 위에 "지남" 으로 안정 정렬한다
        for (ServiceType type : ServiceType.values()) {
            MaintenanceRecord record = latest.get(type);
            if (record != null) {
                results.add(from(type, record, currentOdometer, today));
            }
        }

        results.sort(Comparator.comparing(NextService::overdue).reversed());

        return results;
    }

    /** 같은 종류가 여럿이면 최신 하나. 날짜가 같으면 나중에 넣은 것(id 가 큰 쪽) */
    private static MaintenanceRecord newerOf(MaintenanceRecord kept, MaintenanceRecord candidate) {
        int byDate = candidate.getServiceDate().compareTo(kept.getServiceDate());
        if (byDate != 0) {
            return byDate > 0 ? candidate : kept;
        }

        return candidate.getId() != null && kept.getId() != null && candidate.getId() > kept.getId()
                ? candidate : kept;
    }

    private static NextService from(ServiceType type, MaintenanceRecord record,
                                    int currentOdometer, LocalDate today) {

        Integer intervalKm = type.getRecommendedIntervalKm();
        Integer nextOdometer = (intervalKm == null) ? null : record.getServiceOdometer() + intervalKm;

        Integer intervalMonths = type.getRecommendedIntervalMonths();
        LocalDate nextDate = (intervalMonths == null) ? null
                : record.getServiceDate().plusMonths(intervalMonths);

        // 딱 그 값·그 날이면 지난 것으로 본다 — "오늘까지" 가 아니라 "오늘이 그 날" 이다
        boolean overdue = (nextOdometer != null && currentOdometer >= nextOdometer)
                || (nextDate != null && !today.isBefore(nextDate));

        return new NextService(type, record.getServiceOdometer(), nextOdometer,
                record.getServiceDate(), nextDate, overdue);
    }
}
