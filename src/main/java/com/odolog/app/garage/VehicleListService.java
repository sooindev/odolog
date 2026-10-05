package com.odolog.app.garage;

import com.odolog.app.common.dto.SortGuard;
import com.odolog.app.maintenance.MaintenanceRecordRepository;
import com.odolog.app.maintenance.ServiceIntervalRepository;
import com.odolog.app.maintenance.domain.MaintenanceRecord;
import com.odolog.app.maintenance.domain.NextService;
import com.odolog.app.maintenance.domain.ServiceInterval;
import com.odolog.app.user.UserToday;
import com.odolog.app.vehicle.VehicleRepository;
import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.response.VehicleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 차량 목록 + 지난 정비 수. 읽어서 합치는 조율 층이라 리포지토리 주입
 * 쿼리 5번(페이지·개수·이력·주기·사용자 시간대). 이력·주기는 소유자 단위로 한 번에 조회
 */
@Service
@Transactional(readOnly = true)
public class VehicleListService {

    /** 화면이 쓰는 속성만 정렬 허용 */
    private static final Set<String> SORTABLE = Set.of(
            "createdAt", "plateNumber", "manufacturer", "modelName", "modelYear", "odometer");

    private final VehicleRepository vehicleRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ServiceIntervalRepository serviceIntervalRepository;
    private final UserToday userToday;

    public VehicleListService(VehicleRepository vehicleRepository,
                              MaintenanceRecordRepository maintenanceRecordRepository,
                              ServiceIntervalRepository serviceIntervalRepository,
                              UserToday userToday) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.serviceIntervalRepository = serviceIntervalRepository;
        this.userToday = userToday;
    }

    public Page<VehicleResponse> findMyVehicles(Long ownerId, Pageable pageable) {
        Page<Vehicle> page = vehicleRepository.findByOwnerId(ownerId, SortGuard.allowOnly(pageable, SORTABLE));
        if (page.isEmpty()) {
            return page.map(vehicle -> VehicleResponse.of(vehicle, 0, 0));
        }

        List<MaintenanceRecord> records =
                maintenanceRecordRepository.findByVehicle_Owner_IdOrderByServiceDateDescIdDesc(ownerId);
        List<ServiceInterval> intervals = serviceIntervalRepository.findByVehicle_Owner_Id(ownerId);
        LocalDate today = userToday.of(ownerId);

        return page.map(vehicle -> {
            NextService.Counts counts = countsOf(vehicle, records, intervals, today);
            return VehicleResponse.of(vehicle, counts.overdue(), counts.dueSoon());
        });
    }

    /** 홈 요약과 같은 NextService 계산 */
    private NextService.Counts countsOf(Vehicle vehicle, List<MaintenanceRecord> records,
                               List<ServiceInterval> intervals, LocalDate today) {
        // getId() 는 LAZY 프록시 초기화 없이 조회
        List<MaintenanceRecord> mine = records.stream()
                .filter(record -> record.getVehicle().getId().equals(vehicle.getId()))
                .toList();
        List<ServiceInterval> mineIntervals = intervals.stream()
                .filter(interval -> interval.getVehicle().getId().equals(vehicle.getId()))
                .toList();

        return NextService.count(mine, mineIntervals, vehicle.getOdometer(), today);
    }
}
