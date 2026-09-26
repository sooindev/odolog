package com.odolog.app.account.controller.rest;

import com.odolog.app.common.auth.session.LoginSessionRegistry;
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
 * 경로는 /api/users/me, 패키지는 account
 * UserController 에 두면 user → account 순환
 */
@RestController
public class AccountController {

    private final AccountWithdrawalService accountWithdrawalService;
    private final AccountExportService accountExportService;
    private final AccountRestoreService accountRestoreService;
    private final LoginSessionRegistry sessionRegistry;

    public AccountController(AccountWithdrawalService accountWithdrawalService,
                             AccountExportService accountExportService,
                             AccountRestoreService accountRestoreService,
                             LoginSessionRegistry sessionRegistry) {
        this.accountWithdrawalService = accountWithdrawalService;
        this.accountExportService = accountExportService;
        this.accountRestoreService = accountRestoreService;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * 계정 기록 전체 내보내기
     * 오늘 날짜는 여기서 생성. 서비스 안의 now() 는 테스트 고정 불가
     */
    @GetMapping("/api/users/me/export")
    public AccountExportResponse export(@LoginUser Long userId) {
        return accountExportService.export(userId, LocalDateTime.now());
    }

    /** 내보낸 JSON 복원. 같은 기록은 건너뜀, 하나라도 검증 실패면 전체 취소 */
    @PostMapping("/api/users/me/restore")
    public AccountRestoreResponse restore(@Valid @RequestBody AccountRestoreRequest request,
                                            @LoginUser Long userId) {
        return accountRestoreService.restore(userId, request);
    }

    /** 본문 있는 DELETE. URL 의 비밀번호는 접근 로그·브라우저 기록에 남음 */
    @DeleteMapping("/api/users/me")
    public ResponseEntity<Void> withdraw(@Valid @RequestBody WithdrawRequest request,
                                           @LoginUser Long userId,
                                           HttpServletRequest httpRequest) {
        accountWithdrawalService.withdraw(userId, request);

        // 현재 세션 종료. 없는 사용자 id 가 남으면 다음 요청 500
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // 다른 기기 세션까지 종료
        sessionRegistry.invalidateAll(userId);

        return ResponseEntity.noContent().build();
    }
}
