package com.odolog.app.vehicle.controller;

import com.odolog.app.vehicle.domain.Vehicle;
import com.odolog.app.vehicle.dto.UpdateOdometerRequest;
import com.odolog.app.vehicle.dto.VehicleRegisterRequest;
import com.odolog.app.vehicle.dto.VehicleResponse;
import com.odolog.app.vehicle.service.VehicleService;
import com.odolog.app.common.auth.LoginUser;
import com.odolog.app.common.dto.PageResponse;
import jakarta.validation.Valid;
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
