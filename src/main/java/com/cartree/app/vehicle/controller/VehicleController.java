package com.cartree.app.vehicle.controller;

import com.cartree.app.vehicle.domain.Vehicle;
import com.cartree.app.vehicle.dto.UpdateOdometerRequest;
import com.cartree.app.vehicle.dto.VehicleRegisterRequest;
import com.cartree.app.vehicle.dto.VehicleResponse;
import com.cartree.app.vehicle.service.VehicleService;
import com.cartree.app.common.auth.LoginUser;
import jakarta.validation.Valid;
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
    public ResponseEntity<List<VehicleResponse>> findMyVehicles(@LoginUser Long ownerId) {
        List<VehicleResponse> vehicles = vehicleService.findMyVehicles(ownerId).stream()
                .map(VehicleResponse::from)
                .toList();

        return ResponseEntity.ok(vehicles);
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
