package com.cartree.app.controller;

import com.cartree.app.domain.MaintenanceRecord;
import com.cartree.app.domain.ServiceType;
import com.cartree.app.dto.MaintenanceRecordRegisterRequest;
import com.cartree.app.dto.MaintenanceRecordResponse;
import com.cartree.app.dto.NextServiceResponse;
import com.cartree.app.exception.AuthenticationFailedException;
import com.cartree.app.service.MaintenanceRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
                                                                HttpServletRequest httpRequest) {
        Long requesterId = extractLoginUserId(httpRequest);
        MaintenanceRecord record = maintenanceRecordService.register(requesterId, vehicleId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRecordResponse.from(record));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRecordResponse>> findByVehicle(@PathVariable Long vehicleId,
                                                                          HttpServletRequest httpRequest) {
        Long requesterId = extractLoginUserId(httpRequest);
        List<MaintenanceRecordResponse> records = maintenanceRecordService.findByVehicle(requesterId, vehicleId)
                .stream()
                .map(MaintenanceRecordResponse::from)
                .toList();

        return ResponseEntity.ok(records);
    }

    @GetMapping("/next-service")
    public ResponseEntity<NextServiceResponse> nextService(@PathVariable Long vehicleId,
                                                             @RequestParam ServiceType type,
                                                             HttpServletRequest httpRequest) {
        Long requesterId = extractLoginUserId(httpRequest);
        NextServiceResponse response = maintenanceRecordService.calculateNextService(requesterId, vehicleId, type);

        return ResponseEntity.ok(response);
    }

    private Long extractLoginUserId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(SessionConst.LOGIN_USER_ID) == null) {
            throw new AuthenticationFailedException("로그인이 필요합니다.");
        }

        return (Long) session.getAttribute(SessionConst.LOGIN_USER_ID);
    }
}
