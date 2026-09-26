package com.odolog.app.common.web.header;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** 필터 직접 호출. CsrfTokenFilterTest 와 같은 방식 */
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
        // 재설정 토큰이 주소에 실려 Referer 미전송
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("http 요청에는 HSTS 를 붙이지 않는다")
    void skipsHstsOnPlainHttp() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/users/me"), response, new MockFilterChain());

        // http 에서 HSTS 금지. 도메인 전체가 https 전용이 되는 문제
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
