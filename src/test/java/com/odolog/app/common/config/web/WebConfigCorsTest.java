package com.odolog.app.common.config.web;

import com.odolog.app.common.auth.csrf.CsrfTokenFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** CORS 필터가 CSRF 필터보다 앞인지. 뒤면 CSRF 403 에 CORS 헤더 누락 */
class WebConfigCorsTest {

    private static final String ORIGIN = "http://localhost:5173";

    private final WebConfig webConfig = new WebConfig();

    @Test
    @DisplayName("CORS 필터는 가장 먼저 돈다")
    void corsFilterRunsFirst() {
        assertThat(webConfig.corsFilter().getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    @DisplayName("CSRF 가 막은 403 에도 CORS 헤더가 붙는다")
    void csrfRejectionCarriesCorsHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
        request.addHeader("Origin", ORIGIN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 실제 등록 순서(CORS → CSRF)
        Filter cors = webConfig.corsFilter().getFilter();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
        }, cors, new CsrfTokenFilter(false));

        chain.doFilter(request, response);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
    }
}
