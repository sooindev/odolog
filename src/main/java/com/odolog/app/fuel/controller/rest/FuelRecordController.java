package com.odolog.app.fuel.controller.rest;

import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.common.dto.response.page.PageResponse;
import com.odolog.app.fuel.dto.request.register.FuelRecordRegisterRequest;
import com.odolog.app.fuel.dto.request.update.FuelRecordUpdateRequest;
import com.odolog.app.fuel.dto.response.record.FuelRecordResponse;
import com.odolog.app.fuel.dto.response.summary.FuelSummaryResponse;
import com.odolog.app.fuel.service.application.FuelRecordService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/vehicles/{vehicleId}/fuel-records")
public class FuelRecordController {

    private final FuelRecordService fuelRecordService;

    public FuelRecordController(FuelRecordService fuelRecordService) {
        this.fuelRecordService = fuelRecordService;
    }

    @PostMapping
    public ResponseEntity<FuelRecordResponse> register(@PathVariable Long vehicleId,
                                                         @Valid @RequestBody FuelRecordRegisterRequest request,
                                                         @LoginUser Long requesterId) {
        FuelRecordResponse record = fuelRecordService.register(requesterId, vehicleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * 다른 목록 API 와 달리 sort 를 받지 않는다. 정렬이 곧 연비 계산의 전제라 서비스가 고정한다
     * (자세한 이유는 FuelRecordService.FIXED_SORT 주석에).
     */
    @GetMapping
    public ResponseEntity<PageResponse<FuelRecordResponse>> findByVehicle(
            @PathVariable Long vehicleId,
            @LoginUser Long requesterId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(PageResponse.from(
                fuelRecordService.findByVehicle(requesterId, vehicleId, pageable)));
    }

    // 리터럴 경로가 {recordId} 보다 먼저 매칭된다 — maintenance 의 /next-service 와 같다.
    @GetMapping("/summary")
    public ResponseEntity<FuelSummaryResponse> summary(@PathVariable Long vehicleId,
                                                         @LoginUser Long requesterId) {
        return ResponseEntity.ok(fuelRecordService.summary(requesterId, vehicleId));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<FuelRecordResponse> findOne(@PathVariable Long vehicleId,
                                                        @PathVariable Long recordId,
                                                        @LoginUser Long requesterId) {
        return ResponseEntity.ok(fuelRecordService.findOne(requesterId, vehicleId, recordId));
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<FuelRecordResponse> update(@PathVariable Long vehicleId,
                                                       @PathVariable Long recordId,
                                                       @Valid @RequestBody FuelRecordUpdateRequest request,
                                                       @LoginUser Long requesterId) {
        return ResponseEntity.ok(fuelRecordService.update(requesterId, vehicleId, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@PathVariable Long vehicleId,
                                         @PathVariable Long recordId,
                                         @LoginUser Long requesterId) {
        fuelRecordService.delete(requesterId, vehicleId, recordId);
        return ResponseEntity.noContent().build();
    }
}
