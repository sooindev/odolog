package com.odolog.app.maintenance.controller.rest;

import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
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
    public ResponseEntity<MaintenanceRecordResponse> register(@PathVariable Long vehicleId,
                                                                @Valid @RequestBody MaintenanceRecordRegisterRequest request,
                                                                @LoginUser Long requesterId) {
        MaintenanceRecord record = maintenanceRecordService.register(requesterId, vehicleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRecordResponse.from(record));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MaintenanceRecordResponse>> findByVehicle(
            @PathVariable Long vehicleId,
            @LoginUser Long requesterId,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"serviceDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<MaintenanceRecordResponse> records = PageResponse.from(
                maintenanceRecordService.findByVehicle(requesterId, vehicleId, pageable)
                        .map(MaintenanceRecordResponse::from));

        return ResponseEntity.ok(records);
    }

    /**
     * 종류 전체를 한 번에. 이력이 있는 종류만 담겨 온다.
     *
     * <p>/next-service(단수)는 종류 하나만 묻는 기존 엔드포인트라 그대로 둔다 —
     * 경로가 달라 충돌하지 않고, 특정 종류만 알고 싶을 때는 이쪽이 싸다.
     */
    @GetMapping("/next-services")
    public ResponseEntity<List<NextServiceResponse>> calculateAllNextServices(
            @PathVariable Long vehicleId, @LoginUser Long requesterId) {
        return ResponseEntity.ok(maintenanceRecordService.calculateAllNextServices(requesterId, vehicleId));
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<MaintenanceRecordResponse> update(@PathVariable Long vehicleId,
                                                              @PathVariable Long recordId,
                                                              @Valid @RequestBody MaintenanceRecordUpdateRequest request,
                                                              @LoginUser Long requesterId) {
        MaintenanceRecord record = maintenanceRecordService.update(requesterId, vehicleId, recordId, request);
        return ResponseEntity.ok(MaintenanceRecordResponse.from(record));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@PathVariable Long vehicleId,
                                        @PathVariable Long recordId,
                                        @LoginUser Long requesterId) {
        maintenanceRecordService.delete(requesterId, vehicleId, recordId);
        return ResponseEntity.noContent().build();
    }
}
