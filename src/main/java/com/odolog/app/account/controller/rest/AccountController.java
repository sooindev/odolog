package com.odolog.app.account.controller.rest;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.account.service.application.AccountWithdrawalService;
import com.odolog.app.common.auth.annotation.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 경로는 /api/users/me 지만 패키지는 account
 * UserController 에 두면 user 가 account 를 알게 되어 순환
 */
@RestController
public class AccountController {

    private final AccountWithdrawalService accountWithdrawalService;

    public AccountController(AccountWithdrawalService accountWithdrawalService) {
        this.accountWithdrawalService = accountWithdrawalService;
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
