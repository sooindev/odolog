package com.odolog.app.summary.controller.rest;

import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse;
import com.odolog.app.summary.service.application.GarageSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** /api/vehicles/summary 가 아닌 이유 — 정비·주유까지 담아 차량의 하위 자원이 아님 */
@RestController
public class GarageSummaryController {

    private final GarageSummaryService garageSummaryService;

    public GarageSummaryController(GarageSummaryService garageSummaryService) {
        this.garageSummaryService = garageSummaryService;
    }

    @GetMapping("/api/summary")
    public ResponseEntity<GarageSummaryResponse> summary(@LoginUser Long userId) {
        // "오늘"은 서비스 밖에서 주입. 안에서 now() 를 부르면 월별 12칸을 테스트에서 못 고정
        return ResponseEntity.ok(garageSummaryService.summarize(userId, LocalDate.now()));
    }
}
