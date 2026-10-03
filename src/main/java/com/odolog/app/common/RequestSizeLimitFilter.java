package com.odolog.app.common;

import com.odolog.app.common.validation.InputLimits;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청 본문 크기 상한. Jackson 이 본문 전체를 객체로 만든 뒤에야 @Size 가 돌아서 그 전에 차단
 * 길이를 밝힌 요청은 413, 길이 없이 흘려보내는 요청은 상한에서 읽기 실패(400)
 */
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public RequestSizeLimitFilter() {
        this(InputLimits.MAX_BODY_BYTES);
    }

    RequestSizeLimitFilter(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (request.getContentLengthLong() > maxBytes) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"PAYLOAD_TOO_LARGE\","
                    + "\"message\":\"요청이 너무 큽니다.\"}");
            return;
        }

        chain.doFilter(new LimitedRequest(request, maxBytes), response);
    }

    /** 길이를 밝히지 않은 본문도 상한까지만 읽음 */
    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long maxBytes;
        private ServletInputStream limited;

        LimitedRequest(HttpServletRequest request, long maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (limited == null) {
                limited = new LimitedInputStream(super.getInputStream(), maxBytes);
            }
            return limited;
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream source;
        private final long maxBytes;
        private long read;

        LimitedInputStream(ServletInputStream source, long maxBytes) {
            this.source = source;
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = source.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = source.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(int n) throws IOException {
            read += n;
            if (read > maxBytes) {
                throw new IOException("요청 본문이 상한을 넘었습니다: " + maxBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return source.isFinished();
        }

        @Override
        public boolean isReady() {
            return source.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            source.setReadListener(listener);
        }
    }
}
