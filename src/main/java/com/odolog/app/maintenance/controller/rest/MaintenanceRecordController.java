package com.odolog.app.maintenance.controller.rest;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.type.ServiceType;
import com.odolog.app.maintenance.dto.request.interval.ServiceIntervalRequest;
import com.odolog.app.maintenance.dto.request.register.MaintenanceRecordRegisterRequest;
import com.odolog.app.maintenance.dto.response.record.MaintenanceRecordResponse;
import com.odolog.app.maintenance.dto.request.update.MaintenanceRecordUpdateRequest;
import com.odolog.app.maintenance.dto.response.schedule.NextServiceResponse;
import com.odolog.app.maintenance.service.application.MaintenanceRecordService;
import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.common.dto.response.page.PageResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/maintenance-records")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecordResponse> register(@PathVariable String vehicleId,
                                                                @Valid @RequestBody MaintenanceRecordRegisterRequest request,
                                                                @LoginUser Long requesterId) {
        MaintenanceRecord record = maintenanceRecordService.register(requesterId, vehicleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRecordResponse.from(record));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MaintenanceRecordResponse>> findByVehicle(
            @PathVariable String vehicleId,
            @LoginUser Long requesterId,
            /** 없으면 전체. 잘못된 값은 MethodArgumentTypeMismatch 로 400 */
            @RequestParam(required = false) ServiceType type,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"serviceDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<MaintenanceRecordResponse> records = PageResponse.from(
                maintenanceRecordService.findByVehicle(requesterId, vehicleId, type, pageable)
                        .map(MaintenanceRecordResponse::from));

        return ResponseEntity.ok(records);
    }

    /** 종류 전체를 한 번에. 이력 있는 종류만 반환 */
    @GetMapping("/next-services")
    public ResponseEntity<List<NextServiceResponse>> calculateAllNextServices(
            @PathVariable String vehicleId, @LoginUser Long requesterId) {
        return ResponseEntity.ok(maintenanceRecordService.calculateAllNextServices(requesterId, vehicleId));
    }

    /**
     * 이 차량에서 쓸 권장 주기를 정한다. 204 — 돌려줄 것이 없고, 화면은 곧바로
     * /next-services 를 다시 불러 바뀐 결과를 받는다
     *
     * 뜻은 전체 교체지만 PATCH 를 쓴다 — PUT 을 열면 CORS 의 allowedMethods 와 fetch 래퍼에
     * 메서드를 하나 더 늘려야 하는데, 이 앱에 PUT 이 필요한 자리가 여기뿐이라 값을 못 한다.
     * 대신 화면이 두 칸을 언제나 함께 보낸다 — 둘 다 비워 보내면 기본값으로 되돌아간다
     */
    @PatchMapping("/intervals/{type}")
    public ResponseEntity<Void> changeInterval(@PathVariable String vehicleId,
                                                 @PathVariable ServiceType type,
                                                 @Valid @RequestBody ServiceIntervalRequest request,
                                                 @LoginUser Long requesterId) {
        maintenanceRecordService.changeInterval(requesterId, vehicleId, type,
                request.intervalKm(), request.intervalMonths());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<MaintenanceRecordResponse> update(@PathVariable String vehicleId,
                                                              @PathVariable Long recordId,
                                                              @Valid @RequestBody MaintenanceRecordUpdateRequest request,
                                                              @LoginUser Long requesterId) {
        MaintenanceRecord record = maintenanceRecordService.update(requesterId, vehicleId, recordId, request);
        return ResponseEntity.ok(MaintenanceRecordResponse.from(record));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@PathVariable String vehicleId,
                                        @PathVariable Long recordId,
                                        @LoginUser Long requesterId) {
        maintenanceRecordService.delete(requesterId, vehicleId, recordId);
        return ResponseEntity.noContent().build();
    }
}
