package com.odolog.app.common.web.header;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 응답에 붙는 보안 헤더
 *
 * 스프링 시큐리티를 안 쓰므로(암호화 모듈만 쓴다) 보통 공짜로 따라오는 헤더가 하나도 없다.
 * 그 자리를 이 필터가 대신한다
 *
 * config/web 이 아니라 여기 있는 이유: WebConfig 는 스프링에게 한 번 알려주는 설정이고,
 * 이쪽은 요청마다 실제로 도는 실행 코드다
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 본문을 쓰기 전에 붙인다. 한 번 나간(커밋된) 응답에는 헤더를 더 넣을 수 없다
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 우리 응답을 남의 페이지 iframe 안에 못 띄우게. 클릭재킹 방어
        response.setHeader("X-Frame-Options", "DENY");

        // Referer 를 아예 보내지 않는다. strict-origin-when-cross-origin(브라우저 기본값)은
        // 같은 출처 안에서는 경로와 쿼리를 그대로 실어 보내는데,
        // 이 앱에는 ?token= 으로 비밀번호 재설정 토큰이 붙는 주소가 있다
        response.setHeader("Referrer-Policy", "no-referrer");

        // https 로 들어온 요청에만. http 에서는 브라우저가 어차피 무시하고,
        // 켜 둔 채 http 로 서비스하면 "이 도메인은 https 만" 이라는 약속만 남아 접속이 막힌다
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }

        chain.doFilter(request, response);
    }
}
