package com.odolog.app.common.auth.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
 * CSRF 방어. 쿠키 토큰과 헤더 토큰 비교(double submit)
 * 세션 보관 방식 미사용. 비로그인 방문자에게도 세션이 생기는 문제
 * 테스트에서는 비활성. @WebMvcTest 가 필터를 함께 올려 기존 쓰기 테스트가 403 이 됨
 * 필터 자체는 CsrfTokenFilterTest 에서 직접 검증
 */
@ConditionalOnProperty(prefix = "odolog.csrf", name = "enabled", havingValue = "true", matchIfMissing = true)
@Component
public class CsrfTokenFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "XSRF-TOKEN";
    public static final String HEADER_NAME = "X-XSRF-TOKEN";

    /** 상태를 바꾸지 않는 메서드는 검사 제외 */
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final SecureRandom random = new SecureRandom();

    /** 세션 쿠키와 같은 secure 스위치 사용. 한쪽만 https 전용인 조합 방지 */
    private final boolean secureCookie;

    public CsrfTokenFilter(@Value("${server.servlet.session.cookie.secure:false}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = readCookie(request).orElseGet(() -> issue(response));

        if (!SAFE_METHODS.contains(request.getMethod()) && !token.equals(request.getHeader(HEADER_NAME))) {
            // 403. 요청 출처 확인 실패
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"요청을 확인할 수 없습니다. 새로고침 후 다시 시도해 주세요.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /** /api 밖(정적 파일·swagger)은 검사 제외 */
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
        // HttpOnly 금지. 화면이 읽어 헤더에 실어야 하는 값
        cookie.setHttpOnly(false);
        // https 전용 여부. 세션 쿠키와 동일
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);

        // 발급한 요청 자신도 이 토큰을 아는 것으로 간주
        return new String(token.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
