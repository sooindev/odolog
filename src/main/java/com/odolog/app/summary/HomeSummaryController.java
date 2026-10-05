package com.odolog.app.summary;

import com.odolog.app.common.auth.LoginUser;
import com.odolog.app.summary.dto.response.HomeSummaryResponse;
import com.odolog.app.user.UserService;
import com.odolog.app.user.UserToday;
import com.odolog.app.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** /api/summary 경로. 정비·주유까지 담아 차량 하위 자원이 아님 */
@RestController
public class HomeSummaryController {

    private final HomeSummaryService homeSummaryService;
    private final UserToday userToday;
    private final UserService userService;

    public HomeSummaryController(HomeSummaryService homeSummaryService, UserToday userToday,
                                 UserService userService) {
        this.homeSummaryService = homeSummaryService;
        this.userToday = userToday;
        this.userService = userService;
    }

    @GetMapping("/api/summary")
    public ResponseEntity<HomeSummaryResponse> summary(@LoginUser Long userId) {
        // 오늘 날짜는 밖에서 주입. 월별 12칸 테스트 고정용, 사용자 시간대 기준
        // 사용자 한 번 조회로 통화·오늘 둘 다
        User user = userService.findById(userId);
        return ResponseEntity.ok(homeSummaryService.summarize(userId, userToday.of(user), user.getCurrency()));
    }
}
