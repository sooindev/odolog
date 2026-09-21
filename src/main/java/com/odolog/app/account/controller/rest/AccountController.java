package com.odolog.app.account.controller.rest;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.account.dto.response.export.AccountExportResponse;
import com.odolog.app.account.service.application.AccountExportService;
import com.odolog.app.account.service.application.AccountWithdrawalService;
import com.odolog.app.common.auth.annotation.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AccountController(AccountWithdrawalService accountWithdrawalService,
                             AccountExportService accountExportService) {
        this.accountWithdrawalService = accountWithdrawalService;
        this.accountExportService = accountExportService;
    }

    /**
     * 계정의 기록 전부를 내려준다. 탈퇴 전에 챙겨 갈 수 있어야 한다
     * "오늘"을 여기서 만들어 넘긴다 — 서비스가 now() 를 부르면 테스트에서 고정할 수 없다
     */
    @GetMapping("/api/users/me/export")
    public AccountExportResponse export(@LoginUser Long userId) {
        return accountExportService.export(userId, LocalDateTime.now());
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
