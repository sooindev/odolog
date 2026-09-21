package com.odolog.app.common.auth.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * CSRF 방어. 쿠키에 심은 토큰과 헤더로 온 토큰이 같은지만 본다 (double submit)
 *
 * 세션에 보관하는 방식(synchronizer token)을 안 쓴 이유: 토큰을 내주려면 세션이 있어야 하는데,
 * 그러면 로그인하지 않은 방문자에게도 세션이 생긴다. 여기는 비교만 하므로 서버가 아무것도 안 든다
 *
 * 왜 이걸로 충분한가: 공격자의 페이지는 다른 출처라 이 쿠키를 읽을 수 없고
 * (JS 접근은 같은 출처만), 커스텀 헤더는 CORS 사전 요청을 통과해야 붙는데
 * WebConfig 가 localhost:5173 만 허용한다. 즉 토큰을 알아낼 방법도 실을 방법도 없다
 *
 * 한계: 같은 사이트의 하위 도메인이 장악되면 쿠키를 심을 수 있다. 그때는 세션 보관 방식이 맞다
 *
 * 테스트에서는 꺼 둔다. @WebMvcTest 가 Filter 빈을 함께 올리기 때문에, 켜 두면 기존
 * POST·PATCH·DELETE 테스트 30여 개가 토큰이 없다는 이유로 전부 403 이 된다 —
 * 그 테스트들이 확인하려는 것과 무관한 실패라 신호가 아니라 소음이다.
 * 대신 이 필터 자체는 CsrfTokenFilterTest 가 직접 호출해서 검증한다
 */
@ConditionalOnProperty(prefix = "odolog.csrf", name = "enabled", havingValue = "true", matchIfMissing = true)
@Component
public class CsrfTokenFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "XSRF-TOKEN";
    public static final String HEADER_NAME = "X-XSRF-TOKEN";

    /** 서버 상태를 바꾸지 않는 메서드는 검사하지 않는다 */
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = readCookie(request).orElseGet(() -> issue(response));

        if (!SAFE_METHODS.contains(request.getMethod()) && !token.equals(request.getHeader(HEADER_NAME))) {
            // 403. 인증은 됐지만 이 요청을 이 출처에서 보낸 것이 맞는지 확인되지 않았다
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"요청을 확인할 수 없습니다. 새로고침 후 다시 시도해 주세요.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** /api 밖(정적 파일·swagger)은 검사 대상이 아니다 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    private Optional<String> readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private String issue(HttpServletResponse response) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Cookie cookie = new Cookie(COOKIE_NAME, token);
        // HttpOnly 를 주면 안 된다 — 화면이 이 값을 읽어 헤더에 실어야 한다
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        // 발급한 요청 자신도 이 토큰을 아는 것으로 친다 (첫 요청이 GET 이라 검사를 안 받는다)
        return new String(token.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
