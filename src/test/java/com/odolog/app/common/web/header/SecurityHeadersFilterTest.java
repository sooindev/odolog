package com.odolog.app.common.web.header;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** CsrfTokenFilterTest 와 같은 방식 — 필터를 직접 부른다 */
class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    @DisplayName("모든 응답에 기본 보안 헤더를 붙인다")
    void addsBaseHeaders() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/users/me"), response, chain);

        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        // 재설정 토큰이 ?token= 으로 주소에 실리므로 Referer 를 아예 보내지 않는다
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("http 요청에는 HSTS 를 붙이지 않는다")
    void skipsHstsOnPlainHttp() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/users/me"), response, new MockFilterChain());

        // 로컬이 http 다. 붙여 두면 그 도메인 전체가 https 전용이 되어 접속이 막힌다
        assertThat(response.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    @DisplayName("https 요청에만 HSTS 를 붙인다")
    void addsHstsOnHttps() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("Strict-Transport-Security")).isEqualTo("max-age=31536000");
    }
}
