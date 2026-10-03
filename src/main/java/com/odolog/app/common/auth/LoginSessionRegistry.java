package com.odolog.app.common.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * 사용자별 로그인 세션 끊기. 비밀번호 변경·재설정·탈퇴 시 다른 기기 세션 종료용
 * 세션은 DB(Spring Session JDBC)에 있고, 세션마다 사용자 id 를 색인 이름으로 달아 둠
 * 재시작해도 로그인이 유지되고, 끊기도 DB 기준이라 재시작 뒤에도 빠짐없이 끊김
 */
@Component
public class LoginSessionRegistry {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public LoginSessionRegistry(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    /** 로그인 직후. 계정을 바꿔 로그인하면 색인도 새 사용자로 덮어씀 */
    public void register(Long userId, HttpSession session) {
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, String.valueOf(userId));
    }

    /** 탈퇴·재설정용. 그 사용자의 세션 전부 */
    public void invalidateAll(Long userId) {
        invalidateOthers(userId, null);
    }

    /** 비밀번호 변경용. 현재 세션만 유지 */
    public void invalidateOthers(Long userId, HttpSession keep) {
        String keepId = keep == null ? null : keep.getId();
        for (String sessionId : sessions.findByPrincipalName(String.valueOf(userId)).keySet()) {
            if (!sessionId.equals(keepId)) {
                sessions.deleteById(sessionId);
            }
        }
    }
}
