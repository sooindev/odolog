package com.odolog.app.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestSizeLimitFilterTest {

    private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(10);

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
}
