package com.odolog.app.user;

import com.odolog.app.common.auth.ClientIp;
import com.odolog.app.common.auth.LoginAttemptLimiter;
import com.odolog.app.common.auth.LoginSessionRegistry;
import com.odolog.app.common.auth.SessionConst;
import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.common.exception.type.TooManyRequestsException;
import com.odolog.app.user.domain.User;
import com.odolog.app.user.dto.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * 로그인 순서 조율. IP 집계 → 비밀번호 확인 → 세션 저장 → 비밀번호 재확인
 * 트랜잭션 없음. 재확인이 새 트랜잭션이어야 그 사이 커밋된 변경·탈퇴가 보임
 */
@Service
public class LoginService {

    private static final String LOGIN_IP_KEY_PREFIX = "login-ip:";

    private final UserService userService;
    private final LoginAttemptLimiter attemptLimiter;
    private final LoginSessionRegistry sessionRegistry;

    public LoginService(UserService userService, LoginAttemptLimiter attemptLimiter,
                        LoginSessionRegistry sessionRegistry) {
        this.userService = userService;
        this.attemptLimiter = attemptLimiter;
        this.sessionRegistry = sessionRegistry;
    }

    public User login(LoginRequest request, HttpServletRequest httpRequest) {
        // IP 단위도 집계. 이메일 키만으로는 계정을 바꿔 가며 비밀번호를 뿌리는 시도를 못 막음
        String ipKey = LOGIN_IP_KEY_PREFIX + ClientIp.of(httpRequest);
        attemptLimiter.acquireShared(ipKey, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "로그인 시도가 너무 많습니다.");

        User user;
        try {
            user = userService.login(request, ClientIp.of(httpRequest));
        } catch (TooManyRequestsException e) {
            // 계정 쪽 잠금으로 거절된 시도는 IP 집계에서 되돌림. 비밀번호를 맞춰 보지도 않은 시도
            attemptLimiter.refund(ipKey);
            throw e;
        }

        // id 를 먼저 바꾸고 사용자 id 를 저장. 반대면 로그인 전 세션 id(고정 공격으로 심어 둔 것일 수 있음)에
        // 사용자 id 가 먼저 기록됨(세션 저장이 immediate)
        HttpSession session = httpRequest.getSession();
        httpRequest.changeSessionId();
        session.setAttribute(SessionConst.LOGIN_USER_ID, user.getId());
        // 비밀번호 변경 시 다른 기기 세션 종료용 등록
        sessionRegistry.register(user.getId(), session);

        // 저장 뒤 재확인. 맞춰 보는 동안 재설정·변경·탈퇴가 끝났으면 그쪽의 세션 끊기를 이미 지나친 세션
        if (!userService.isPasswordCurrent(user.getId(), user.getPassword())) {
            session.invalidate();
            throw new AuthenticationFailedException(ErrorCode.LOGIN_FAILED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 성공은 IP 한도에서 제외. 공유 IP 가 정상 로그인만으로 잠기지 않게
        attemptLimiter.refund(ipKey);
        return user;
    }
}
