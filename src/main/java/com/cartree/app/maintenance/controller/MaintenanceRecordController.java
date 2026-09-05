package com.cartree.app.maintenance.controller;

import com.cartree.app.maintenance.domain.MaintenanceRecord;
import com.cartree.app.maintenance.domain.ServiceType;
import com.cartree.app.maintenance.dto.MaintenanceRecordRegisterRequest;
import com.cartree.app.maintenance.dto.MaintenanceRecordResponse;
import com.cartree.app.maintenance.dto.NextServiceResponse;
import com.cartree.app.maintenance.service.MaintenanceRecordService;
import com.cartree.app.common.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<List<MaintenanceRecordResponse>> findByVehicle(@PathVariable Long vehicleId,
                                                                          @LoginUser Long requesterId) {
        List<MaintenanceRecordResponse> records = maintenanceRecordService.findByVehicle(requesterId, vehicleId)
                .stream()
                .map(MaintenanceRecordResponse::from)
                .toList();

        return ResponseEntity.ok(records);
    }

    @GetMapping("/next-service")
    public ResponseEntity<NextServiceResponse> nextService(@PathVariable Long vehicleId,
                                                             @RequestParam ServiceType type,
                                                             @LoginUser Long requesterId) {
        NextServiceResponse response = maintenanceRecordService.calculateNextService(requesterId, vehicleId, type);
        return ResponseEntity.ok(response);
    }
}
