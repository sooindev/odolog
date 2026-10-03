package com.odolog.app.common.config;

import com.odolog.app.common.auth.LoginUser;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "오도로그 API",
        version = "v1",
        description = """
                차량 등록과 정비 이력을 관리하는 API.

                인증은 세션 쿠키(JSESSIONID) 기반이다. 먼저 POST /api/users/login 을 실행하면
                브라우저에 쿠키가 저장되어 이후 요청에 자동으로 붙는다."""))
public class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUser.class);
    }
}
