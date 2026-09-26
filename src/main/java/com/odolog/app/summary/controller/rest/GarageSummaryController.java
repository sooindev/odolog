package com.odolog.app.summary.controller.rest;

import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse;
import com.odolog.app.summary.service.application.GarageSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** /api/summary 경로. 정비·주유까지 담아 차량 하위 자원이 아님 */
@RestController
public class GarageSummaryController {

    private final GarageSummaryService garageSummaryService;

    public GarageSummaryController(GarageSummaryService garageSummaryService) {
        this.garageSummaryService = garageSummaryService;
    }

    @GetMapping("/api/summary")
    public ResponseEntity<GarageSummaryResponse> summary(@LoginUser Long userId) {
        // 오늘 날짜는 밖에서 주입. 월별 12칸 테스트 고정용
        return ResponseEntity.ok(garageSummaryService.summarize(userId, LocalDate.now()));
    }
}
