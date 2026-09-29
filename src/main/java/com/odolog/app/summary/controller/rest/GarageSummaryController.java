package com.odolog.app.summary.controller.rest;

import com.odolog.app.common.auth.annotation.LoginUser;
import com.odolog.app.summary.dto.response.garage.GarageSummaryResponse;
import com.odolog.app.summary.service.application.GarageSummaryService;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.user.service.time.UserToday;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/** /api/summary 경로. 정비·주유까지 담아 차량 하위 자원이 아님 */
@RestController
public class GarageSummaryController {

    private final GarageSummaryService garageSummaryService;
    private final UserToday userToday;
    private final UserService userService;

    public GarageSummaryController(GarageSummaryService garageSummaryService, UserToday userToday,
                                   UserService userService) {
        this.garageSummaryService = garageSummaryService;
        this.userToday = userToday;
        this.userService = userService;
    }

    @GetMapping("/api/summary")
    public ResponseEntity<GarageSummaryResponse> summary(@LoginUser Long userId) {
        // 오늘 날짜는 밖에서 주입. 월별 12칸 테스트 고정용, 사용자 시간대 기준
        String currency = userService.findById(userId).getCurrency();
        return ResponseEntity.ok(garageSummaryService.summarize(userId, userToday.of(userId), currency));
    }
}
