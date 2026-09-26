package com.odolog.app.common.auth.session;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 로그인 세션 목록. 비밀번호 변경·재설정·탈퇴 시 다른 기기 세션 종료용
 * 세션과 같은 톰캣 메모리 보관. 재시작 시 함께 초기화
 */
@Component
public class LoginSessionRegistry implements HttpSessionListener {

    private final Map<Long, Set<HttpSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, HttpSession session) {
        // 로그아웃 없는 계정 전환 시 같은 세션 재사용(changeSessionId)
        // 옛 주인 목록에서 제거
        sessions.forEach((owner, owned) -> {
            if (!owner.equals(userId)) {
                owned.remove(session);
            }
        });
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    /** 탈퇴·재설정용. 그 사용자의 세션 전부 */
    public void invalidateAll(Long userId) {
        invalidateOthers(userId, null);
    }

    /** 비밀번호 변경용. 현재 세션만 유지 */
    public void invalidateOthers(Long userId, HttpSession keep) {
        Set<HttpSession> owned = sessions.get(userId);
        if (owned == null) {
            return;
        }

        // 복사본 순회. invalidate() 가 sessionDestroyed 로 원본에서 제거
        for (HttpSession session : List.copyOf(owned)) {
            if (session == keep) {
                continue;
            }
            owned.remove(session);
            try {
                session.invalidate();
            } catch (IllegalStateException alreadyInvalidated) {
                // 그 사이 만료·로그아웃된 세션
            }
        }
    }

    /** 끝난 세션 제거. 목록 무한 증가 방지 */
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession destroyed = event.getSession();
        sessions.forEach((userId, owned) -> {
            owned.remove(destroyed);
            sessions.computeIfPresent(userId, (id, current) -> current.isEmpty() ? null : current);
        });
    }
}
