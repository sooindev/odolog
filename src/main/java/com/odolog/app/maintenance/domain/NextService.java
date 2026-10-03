package com.odolog.app.maintenance.domain;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 다음 정비 시점과 지남 판정. 차량 상세·홈 요약 공용
 *
 * @param overdue    주행거리·날짜 중 하나라도 지남
 * @param intervalKm 실제 적용 주기. 차량별 설정 우선, 없으면 ServiceType 기본값
 * @param customized 기본값 덮어씀 여부
 * @param customIntervalKm     차량별로 직접 정한 km 주기. 기본값을 쓰면 null
 * @param customIntervalMonths 차량별로 직접 정한 개월 주기. 기본값을 쓰면 null
 */
public record NextService(
        ServiceType type,
        Integer lastOdometer,
        Integer nextOdometer,
        LocalDate lastDate,
        LocalDate nextDate,
        boolean overdue,
        Integer intervalKm,
        Integer intervalMonths,
        boolean customized,
        Integer customIntervalKm,
        Integer customIntervalMonths
) {

    /**
     * 종류별 다음 정비 시점. 이력 있는 종류만, 지난 것 먼저
     *
     * @param records         한 차량의 정비 이력(정렬 무관)
     * @param currentOdometer 그 차량의 현재 주행거리
     * @param today           오늘. 테스트 고정용으로 밖에서 주입
     */
    public static List<NextService> of(List<MaintenanceRecord> records,
                                       List<ServiceInterval> overrides,
                                       int currentOdometer, LocalDate today) {

        Map<ServiceType, MaintenanceRecord> latest = new EnumMap<>(ServiceType.class);
        for (MaintenanceRecord record : records) {
            latest.merge(record.getType(), record, NextService::newerOf);
        }

        Map<ServiceType, ServiceInterval> custom = new EnumMap<>(ServiceType.class);
        for (ServiceInterval override : overrides) {
            custom.put(override.getType(), override);
        }

        List<NextService> results = new ArrayList<>(latest.size());
        // enum 선언 순서 위에 지남 여부로 안정 정렬
        for (ServiceType type : ServiceType.values()) {
            MaintenanceRecord record = latest.get(type);
            if (record != null) {
                results.add(from(type, record, custom.get(type), currentOdometer, today));
            }
        }

        results.sort(Comparator.comparing(NextService::overdue).reversed());

        return results;
    }

    /** 지난 정비 수. 차량 목록·홈 요약 공용 */
    public static int overdueCount(List<MaintenanceRecord> records, List<ServiceInterval> overrides,
                                   int currentOdometer, LocalDate today) {
        return (int) of(records, overrides, currentOdometer, today).stream()
                .filter(NextService::overdue)
                .count();
    }

    /** 같은 종류 중 최신 하나. 날짜가 같으면 id 가 큰 쪽 */
    private static MaintenanceRecord newerOf(MaintenanceRecord kept, MaintenanceRecord candidate) {
        int byDate = candidate.getServiceDate().compareTo(kept.getServiceDate());
        if (byDate != 0) {
            return byDate > 0 ? candidate : kept;
        }

        return candidate.getId() != null && kept.getId() != null && candidate.getId() > kept.getId()
                ? candidate : kept;
    }

    private static NextService from(ServiceType type, MaintenanceRecord record,
                                    ServiceInterval override, int currentOdometer, LocalDate today) {

        // km·개월 개별 덮어쓰기
        Integer customKm = override == null ? null : override.getIntervalKm();
        Integer customMonths = override == null ? null : override.getIntervalMonths();
        Integer intervalKm = customKm != null ? customKm : type.getRecommendedIntervalKm();
        Integer intervalMonths = customMonths != null ? customMonths : type.getRecommendedIntervalMonths();

        // 주행거리를 모르는 기록은 날짜 기준만
        Integer nextOdometer = (intervalKm == null || record.getServiceOdometer() == null)
                ? null : record.getServiceOdometer() + intervalKm;
        LocalDate nextDate = (intervalMonths == null) ? null
                : record.getServiceDate().plusMonths(intervalMonths);

        // 딱 그 값·그 날도 지남
        boolean overdue = (nextOdometer != null && currentOdometer >= nextOdometer)
                || (nextDate != null && !today.isBefore(nextDate));

        return new NextService(type, record.getServiceOdometer(), nextOdometer,
                record.getServiceDate(), nextDate, overdue,
                intervalKm, intervalMonths, override != null, customKm, customMonths);
    }
}
