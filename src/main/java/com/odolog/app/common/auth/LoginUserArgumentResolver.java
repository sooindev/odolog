package com.odolog.app.common.auth;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * 애노테이션만 보고 맡음. 타입까지 보면 Long 이 아닌 @LoginUser 를 다른 리졸버가 받아 쿼리 파라미터로 채움
     * 타입이 틀리면 첫 요청에서 500 으로 드러나게
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!Long.class.equals(parameter.getParameterType())) {
            throw new IllegalStateException("@LoginUser 는 Long 에만 붙일 수 있습니다: " + parameter);
        }

        HttpServletRequest httpRequest = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute(SessionConst.LOGIN_USER_ID) == null) {
            throw new AuthenticationFailedException(ErrorCode.LOGIN_REQUIRED, "로그인이 필요합니다.");
        }

        return session.getAttribute(SessionConst.LOGIN_USER_ID);
    }
}
