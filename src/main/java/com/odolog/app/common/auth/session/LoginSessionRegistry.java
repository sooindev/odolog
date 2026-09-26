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
 * 사용자별 로그인 세션 목록. 비밀번호 변경·재설정·탈퇴 때 다른 기기의 세션을 끊는 데 쓴다
 * 세션이 톰캣 메모리에 있으므로 목록도 메모리 — 재시작하면 둘 다 사라져 어긋나지 않는다
 */
@Component
public class LoginSessionRegistry implements HttpSessionListener {

    private final Map<Long, Set<HttpSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, HttpSession session) {
        // 로그아웃 없이 다른 계정으로 로그인하면 같은 세션을 다시 쓴다(changeSessionId).
        // 옛 주인 목록에서 빼지 않으면 그 사람이 비밀번호를 바꿀 때 지금 주인이 로그아웃된다
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

    /** 비밀번호 변경용. 지금 요청한 세션만 남긴다 — 본인이 바꾼 것이라 다시 로그인시킬 이유가 없다 */
    public void invalidateOthers(Long userId, HttpSession keep) {
        Set<HttpSession> owned = sessions.get(userId);
        if (owned == null) {
            return;
        }

        // 복사본을 돈다. invalidate() 가 sessionDestroyed 를 불러 원본에서 지운다
        for (HttpSession session : List.copyOf(owned)) {
            if (session == keep) {
                continue;
            }
            owned.remove(session);
            try {
                session.invalidate();
            } catch (IllegalStateException alreadyInvalidated) {
                // 그 사이 만료·로그아웃된 세션. 목표 상태와 같다
            }
        }
    }

    /** 만료·로그아웃으로 끝난 세션을 목록에서 뺀다. 안 빼면 목록이 계속 자란다 */
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession destroyed = event.getSession();
        sessions.forEach((userId, owned) -> {
            owned.remove(destroyed);
            sessions.computeIfPresent(userId, (id, current) -> current.isEmpty() ? null : current);
        });
    }
}
