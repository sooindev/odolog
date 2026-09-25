package com.odolog.app.account.controller.rest;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.account.dto.request.restore.AccountRestoreRequest;
import com.odolog.app.account.dto.response.export.AccountExportResponse;
import com.odolog.app.account.dto.response.restore.AccountRestoreResponse;
import com.odolog.app.account.service.application.AccountExportService;
import com.odolog.app.account.service.application.AccountRestoreService;
import com.odolog.app.account.service.application.AccountWithdrawalService;
import com.odolog.app.common.auth.annotation.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 경로는 /api/users/me 지만 패키지는 account
 * UserController 에 두면 user 가 account 를 알게 되어 순환
 */
@RestController
public class AccountController {

    private final AccountWithdrawalService accountWithdrawalService;
    private final AccountExportService accountExportService;
    private final AccountRestoreService accountRestoreService;

    public AccountController(AccountWithdrawalService accountWithdrawalService,
                             AccountExportService accountExportService,
                             AccountRestoreService accountRestoreService) {
        this.accountWithdrawalService = accountWithdrawalService;
        this.accountExportService = accountExportService;
        this.accountRestoreService = accountRestoreService;
    }

    /**
     * 계정의 기록 전부를 내려준다. 탈퇴 전에 챙겨 갈 수 있어야 한다
     * "오늘"을 여기서 만들어 넘긴다 — 서비스가 now() 를 부르면 테스트에서 고정할 수 없다
     */
    @GetMapping("/api/users/me/export")
    public AccountExportResponse export(@LoginUser Long userId) {
        return accountExportService.export(userId, LocalDateTime.now());
    }

    /**
     * 내보낸 JSON 을 되돌려 넣는다. 200 + 무엇이 들어갔는지
     *
     * 같은 파일을 두 번 넣어도 두 배가 되지 않는다 — 날짜·주행거리가 같으면 건너뛴다.
     * 하나라도 검증에 걸리면 전부 안 들어간다
     */
    @PostMapping("/api/users/me/restore")
    public AccountRestoreResponse restore(@Valid @RequestBody AccountRestoreRequest request,
                                            @LoginUser Long userId) {
        return accountRestoreService.restore(userId, request);
    }

    /** 본문 있는 DELETE. 비밀번호를 URL 에 넣으면 접근 로그·브라우저 기록에 평문으로 남음 */
    @DeleteMapping("/api/users/me")
    public ResponseEntity<Void> withdraw(@Valid @RequestBody WithdrawRequest request,
                                           @LoginUser Long userId,
                                           HttpServletRequest httpRequest) {
        accountWithdrawalService.withdraw(userId, request);

        // 세션도 끊기. 없는 사용자 id 를 든 세션이 남으면 다음 요청에서 500
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }
}
