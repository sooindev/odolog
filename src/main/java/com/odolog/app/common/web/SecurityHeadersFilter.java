package com.odolog.app.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** 모든 응답의 보안 헤더. 스프링 시큐리티 미사용이라 직접 추가 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 본문 쓰기 전에 추가. 커밋된 응답은 헤더 변경 불가
        response.setHeader("X-Content-Type-Options", "nosniff");

        // iframe 삽입 금지. 클릭재킹 방어
        response.setHeader("X-Frame-Options", "DENY");

        // Referer 미전송. API 응답용 기본값. 화면 주소(재설정 토큰) 보호는 index.html 의 meta 태그가 함
        response.setHeader("Referrer-Policy", "no-referrer");

        // https 요청에만 HSTS. http 에서 켜면 접속 차단
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }

        chain.doFilter(request, response);
    }
}
