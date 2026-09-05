package com.cartree.app.controller;

import com.cartree.app.exception.AuthenticationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest httpRequest = (HttpServletRequest) webRequest.getNativeRequest();
        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute(SessionConst.LOGIN_USER_ID) == null) {
            throw new AuthenticationFailedException("로그인이 필요합니다.");
        }

        return session.getAttribute(SessionConst.LOGIN_USER_ID);
    }
}
