package com.cartree.app.controller;

import com.cartree.app.domain.Vehicle;
import com.cartree.app.dto.VehicleRegisterRequest;
import com.cartree.app.dto.VehicleResponse;
import com.cartree.app.exception.AuthenticationFailedException;
import com.cartree.app.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
                                                      HttpServletRequest httpRequest) {
        Long ownerId = extractLoginUserId(httpRequest);
        Vehicle vehicle = vehicleService.register(ownerId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.from(vehicle));
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> findMyVehicles(HttpServletRequest httpRequest) {
        Long ownerId = extractLoginUserId(httpRequest);
        List<VehicleResponse> vehicles = vehicleService.findMyVehicles(ownerId).stream()
                .map(VehicleResponse::from)
                .toList();

        return ResponseEntity.ok(vehicles);
    }

    private Long extractLoginUserId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute(SessionConst.LOGIN_USER_ID) == null) {
            throw new AuthenticationFailedException("로그인이 필요합니다.");
        }

        return (Long) session.getAttribute(SessionConst.LOGIN_USER_ID);
    }
}
