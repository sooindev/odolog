package com.odolog.app.garage;

import com.odolog.app.common.auth.LoginUser;
import com.odolog.app.common.dto.response.PageResponse;
import com.odolog.app.vehicle.dto.response.VehicleResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 차량 목록·삭제. URL 은 vehicles 지만 다른 기능의 기록까지 다뤄 garage 담당
 * 나머지 차량 API 는 VehicleController
 */
// 두 컨트롤러가 같은 주소를 나눠 맡음. 문서에서는 한 묶음
@Tag(name = "vehicles")
@RestController
@RequestMapping("/api/vehicles")
public class GarageVehicleController {

    private final VehicleListService vehicleListService;
    private final VehicleRemovalService vehicleRemovalService;

    public GarageVehicleController(VehicleListService vehicleListService,
                                   VehicleRemovalService vehicleRemovalService) {
        this.vehicleListService = vehicleListService;
        this.vehicleRemovalService = vehicleRemovalService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<VehicleResponse>> findMyVehicles(
            @LoginUser Long ownerId,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(PageResponse.from(vehicleListService.findMyVehicles(ownerId, pageable)));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(@PathVariable String vehicleId, @LoginUser Long requesterId) {
        vehicleRemovalService.delete(requesterId, vehicleId);
        return ResponseEntity.noContent().build();
    }
}
