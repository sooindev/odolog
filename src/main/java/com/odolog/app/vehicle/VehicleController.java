package com.odolog.app.vehicle;

import com.odolog.app.vehicle.dto.request.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.VehicleUpdateRequest;
import com.odolog.app.vehicle.dto.VehicleResponse;
import com.odolog.app.common.auth.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 목록·삭제는 다른 기능의 기록을 함께 다뤄 garage 가 담당 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> register(@Valid @RequestBody VehicleRegisterRequest request,
                                                      @LoginUser Long ownerId) {
        Vehicle vehicle = vehicleService.register(ownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.from(vehicle));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> findOne(@PathVariable String vehicleId, @LoginUser Long requesterId) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    // 주행거리는 별도 엔드포인트. 감소 금지 규칙
    @PatchMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> update(@PathVariable String vehicleId,
                                                    @Valid @RequestBody VehicleUpdateRequest request,
                                                    @LoginUser Long requesterId) {
        Vehicle vehicle = vehicleService.update(requesterId, vehicleId, request);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @PatchMapping("/{vehicleId}/odometer")
    public ResponseEntity<VehicleResponse> updateOdometer(@PathVariable String vehicleId,
                                                            @Valid @RequestBody UpdateOdometerRequest request,
                                                            @LoginUser Long requesterId) {
        Vehicle vehicle = vehicleService.updateOdometer(requesterId, vehicleId, request);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }
}
