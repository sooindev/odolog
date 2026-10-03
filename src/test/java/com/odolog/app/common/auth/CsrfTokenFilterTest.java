package com.odolog.app.common.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 필터 직접 호출. @WebMvcTest 에 필터를 올리면 기존 쓰기 테스트가 403 */
class CsrfTokenFilterTest {

    // 로컬과 같은 http 설정. secure 는 마지막 테스트
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
        // 화면이 읽어야 하는 값이라 HttpOnly 금지
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
        // 필터는 핸들러를 안 거쳐 본문을 직접 씀. 코드가 빠지면 화면이 번역 못 함
        assertThat(response.getContentAsString()).contains("\"code\":\"CSRF_REJECTED\"");
        // 다음 필터로 진행하지 않음
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

        // 세션 쿠키와 같은 secure 설정
        assertThat(response.getCookie(CsrfTokenFilter.COOKIE_NAME).getSecure()).isTrue();
    }

    @Test
    @DisplayName("경로와 무관하게 쓰기 요청은 검사한다 — 인코딩·경로 매개변수로 /api 비교를 비껴가지 못하게")
    void checksEncodedPaths() throws Exception {
        // 스프링은 /%61pi/users 와 /api;x=1/users 를 /api/users 로 라우팅
        for (String uri : new String[]{"/%61pi/users", "/api;x=1/users", "/swagger-ui.html"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request("POST", uri), response, chain);

            assertThat(response.getStatus()).as(uri).isEqualTo(403);
            assertThat(chain.getRequest()).as(uri).isNull();
        }
    }
}
