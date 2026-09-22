package com.odolog.app.common.auth.csrf;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 필터를 직접 호출해서 본다
 * @WebMvcTest 로 하면 Filter 빈이 같이 올라와 기존 테스트 30여 개가 403 이 된다
 */
class CsrfTokenFilterTest {

    // 로컬과 같은 설정(http). Secure 를 켠 경우는 마지막 테스트에서 따로 본다
    private final CsrfTokenFilter filter = new CsrfTokenFilter(false);

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    @Test
    @DisplayName("첫 GET 요청에 토큰 쿠키를 내려준다")
    void issuesTokenOnFirstRequest() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/users/me"), response, chain);

        Cookie cookie = response.getCookie(CsrfTokenFilter.COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        // 화면이 읽어 헤더에 실어야 하므로 HttpOnly 면 안 된다
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("GET 은 토큰이 없어도 통과시킨다")
    void allowsSafeMethodsWithoutToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("GET", "/api/vehicles"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("헤더가 없는 POST 는 403 으로 막는다")
    void blocksMutatingRequestWithoutHeader() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/vehicles");
        request.setCookies(new Cookie(CsrfTokenFilter.COOKIE_NAME, "token-value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        // 다음 필터로 넘어가지 않아야 한다 — 넘어가면 이미 실행된 것이다
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("헤더와 쿠키가 다르면 403 으로 막는다")
    void blocksMismatchedToken() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/vehicles");
        request.setCookies(new Cookie(CsrfTokenFilter.COOKIE_NAME, "token-value"));
        request.addHeader(CsrfTokenFilter.HEADER_NAME, "다른-값");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("헤더와 쿠키가 같으면 통과시킨다")
    void allowsMatchingToken() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/vehicles");
        request.setCookies(new Cookie(CsrfTokenFilter.COOKIE_NAME, "token-value"));
        request.addHeader(CsrfTokenFilter.HEADER_NAME, "token-value");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("secure 를 켜면 토큰 쿠키도 https 전용이 된다")
    void issuesSecureCookieWhenEnabled() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CsrfTokenFilter(true).doFilter(request("GET", "/api/users/me"), response, new MockFilterChain());

        // 세션 쿠키만 지키고 이쪽이 평문으로 나가면 반쪽짜리다
        assertThat(response.getCookie(CsrfTokenFilter.COOKIE_NAME).getSecure()).isTrue();
    }

    @Test
    @DisplayName("/api 밖은 검사하지 않는다")
    void skipsNonApiPaths() throws Exception {
        // swagger-ui 와 정적 파일까지 막으면 문서를 못 연다
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request("POST", "/swagger-ui.html"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
