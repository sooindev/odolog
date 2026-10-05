package com.odolog.app.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.filter.OrderedFormContentFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestSizeLimitFilterTest {

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);

    @Test
    @DisplayName("본문을 먼저 읽는 FormContentFilter 보다 앞, CORS 보다 뒤")
    void runsBeforeFormContentFilter() {
        int order = RequestSizeLimitFilter.class.getAnnotation(Order.class).value();

        assertThat(order).isLessThan(OrderedFormContentFilter.DEFAULT_ORDER);
        assertThat(order).isGreaterThan(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    @DisplayName("길이를 밝힌 본문이 상한을 넘으면 읽기 전에 413")
    void rejectsDeclaredOversizedBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
        request.setContent(new byte[11]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    @DisplayName("길이를 밝히지 않은 본문도 상한까지만 읽는다")
    void cutsUndeclaredOversizedBody() throws Exception {
        // chunked 처럼 길이 없는 요청
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(new byte[11]);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        InputStream body = chain.getRequest().getInputStream();
        assertThatThrownBy(body::readAllBytes).isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("상한 안의 본문은 그대로 읽힌다")
    void passesSmallBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users/login");
        request.setContent(new byte[10]);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest().getInputStream().readAllBytes()).hasSize(10);
    }

    @Test
    @DisplayName("문자로 읽어도 상한에서 끊긴다")
    void limitsReader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/users/me") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(new byte[11]);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        BufferedReader reader = chain.getRequest().getReader();
        assertThatThrownBy(() -> reader.lines().count()).hasRootCauseInstanceOf(IOException.class);
    }
}
