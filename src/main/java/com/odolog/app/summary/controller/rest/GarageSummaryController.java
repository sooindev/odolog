package com.odolog.app.summary.controller.rest;

import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse;
import com.odolog.app.summary.service.application.GarageSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 경로가 /api/vehicles/summary 가 아니라 /api/summary 인 이유:
 * 이 응답은 차량뿐 아니라 정비 이력과 주유 기록까지 담는다. 차량의 하위 자원이 아니다.
 */
@RestController
public class GarageSummaryController {

    private final GarageSummaryService garageSummaryService;

    public GarageSummaryController(GarageSummaryService garageSummaryService) {
        this.garageSummaryService = garageSummaryService;
    }

    @GetMapping("/api/summary")
    public ResponseEntity<GarageSummaryResponse> summary(@LoginUser Long userId) {
        // "오늘"을 서비스 밖에서 정한다. 안에서 LocalDate.now() 를 부르면 월별 12칸이
        // 실행 시각에 좌우되어 테스트에서 고정할 수가 없다.
        return ResponseEntity.ok(garageSummaryService.summarize(userId, LocalDate.now()));
    }
}
