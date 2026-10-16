package com.odolog.app.common.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 세션의 사용자 id 주입. 요청 값으로 채워지는 길이 없어야 함 */
class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    @SuppressWarnings("unused")
    void handler(@LoginUser Long ok, @LoginUser String wrong) {
    }

    private MethodParameter parameter(int index) throws NoSuchMethodException {
        return new MethodParameter(getClass().getDeclaredMethod("handler", Long.class, String.class), index);
    }

    @Test
    @DisplayName("Long 이 아닌 @LoginUser 도 이 리졸버가 맡아 500 으로 막는다 — 다른 리졸버가 쿼리 파라미터로 채우지 않게")
    void rejectsNonLongParameter() throws Exception {
        MethodParameter wrong = parameter(1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SessionConst.LOGIN_USER_ID, 1L);
        request.setParameter("wrong", "2");

        assertThat(resolver.supportsParameter(wrong)).isTrue();
        assertThatThrownBy(() -> resolver.resolveArgument(wrong, null, new ServletWebRequest(request), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Long 이면 세션의 사용자 id")
    void resolvesFromSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SessionConst.LOGIN_USER_ID, 7L);

        assertThat(resolver.resolveArgument(parameter(0), null, new ServletWebRequest(request), null)).isEqualTo(7L);
    }
}
