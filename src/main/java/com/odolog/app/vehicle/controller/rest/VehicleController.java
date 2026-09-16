package com.odolog.app.vehicle.controller.rest;

import com.odolog.app.vehicle.domain.entity.Vehicle;
import com.odolog.app.vehicle.dto.request.odometer.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.request.register.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.request.update.VehicleUpdateRequest;
import com.odolog.app.vehicle.dto.response.vehicle.VehicleResponse;
import com.odolog.app.vehicle.service.application.VehicleService;
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

    @GetMapping
    public ResponseEntity<PageResponse<VehicleResponse>> findMyVehicles(
            @LoginUser Long ownerId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<VehicleResponse> vehicles = PageResponse.from(
                vehicleService.findMyVehicles(ownerId, pageable).map(VehicleResponse::from));

        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> findOne(@PathVariable Long vehicleId, @LoginUser Long requesterId) {
        Vehicle vehicle = vehicleService.findOwnedVehicle(requesterId, vehicleId);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    // PATCH /{vehicleId} 와 PATCH /{vehicleId}/odometer 는 경로가 달라 충돌하지 않는다.
    // 주행거리를 여기 합치지 않은 이유: 감소 금지라는 규칙이 붙어 있어 성격이 다르고,
    // 화면에서도 "차량 정보 고치기"와 "주행거리 갱신"은 서로 다른 순간에 일어난다.
    @PatchMapping("/{vehicleId}")
    public ResponseEntity<VehicleResponse> update(@PathVariable Long vehicleId,
                                                    @Valid @RequestBody VehicleUpdateRequest request,
                                                    @LoginUser Long requesterId) {
        Vehicle vehicle = vehicleService.update(requesterId, vehicleId, request);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @PatchMapping("/{vehicleId}/odometer")
    public ResponseEntity<VehicleResponse> updateOdometer(@PathVariable Long vehicleId,
                                                            @Valid @RequestBody UpdateOdometerRequest request,
                                                            @LoginUser Long requesterId) {
        Vehicle vehicle = vehicleService.updateOdometer(requesterId, vehicleId, request);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(@PathVariable Long vehicleId, @LoginUser Long requesterId) {
        vehicleService.delete(requesterId, vehicleId);
        return ResponseEntity.noContent().build();
    }
}
