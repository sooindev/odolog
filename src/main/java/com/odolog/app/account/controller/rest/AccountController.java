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
 * 경로는 /api/users/me 이지만 컨트롤러는 account 패키지에 있다.
 * URL 은 클라이언트가 보는 주소이고 패키지는 코드의 소속이라, 둘이 꼭 같을 필요는 없다.
 * UserController 에 두면 user 패키지가 account 를 알게 되어 순환이 된다.
 */
@RestController
public class AccountController {

    private final AccountWithdrawalService accountWithdrawalService;

    public AccountController(AccountWithdrawalService accountWithdrawalService) {
        this.accountWithdrawalService = accountWithdrawalService;
    }

    /**
     * DELETE 에 본문을 싣는다. 비밀번호를 쿼리 파라미터에 넣으면 서버 접근 로그와 브라우저
     * 기록에 평문으로 남는다. 본문 있는 DELETE 를 꺼리는 중간 장비가 있지만,
     * 비밀번호를 URL 에 노출하는 것보다는 그쪽이 낫다.
     */
    @DeleteMapping("/api/users/me")
    public ResponseEntity<Void> withdraw(@Valid @RequestBody WithdrawRequest request,
                                           @LoginUser Long userId,
                                           HttpServletRequest httpRequest) {
        accountWithdrawalService.withdraw(userId, request);

        // 계정이 사라졌으므로 세션도 끊는다. 안 끊으면 없는 사용자 id 를 든 세션이 남아,
        // 다음 요청에서 findById 가 IllegalStateException → 500 이 된다.
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
    }
}
