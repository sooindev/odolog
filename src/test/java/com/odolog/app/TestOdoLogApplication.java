package com.odolog.app;

import org.springframework.boot.SpringApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * E2E(Playwright) 용 백엔드. `./gradlew bootTestRun` 으로 뜬다
 * 테스트 설정(src/test/resources) 그대로라 DB 는 odolog_test, 뜰 때마다 빈 스키마(create-drop)
 * 운영 DB·평소 개발 서버(8080)와 섞이지 않게 포트도 따로
 */
public class TestOdoLogApplication {

    public static void main(String[] args) {
        List<String> all = new ArrayList<>(List.of(
                "--server.port=18080",
                // E2E 화면은 5174. 평소 개발 서버(5173)와 겹치지 않게
                "--odolog.cors.allowed-origins=http://localhost:5174",
                // 단위 테스트는 끄지만 E2E 는 운영처럼 켬
                "--odolog.csrf.enabled=true",
                "--odolog.app.base-url=http://localhost:5174",
                "--odolog.mail.log-link-on-failure=true",
                // 한 IP 에서 테스트마다 새 계정을 만듦. 운영 한도(10)면 열한 번째 가입부터 429
                "--odolog.rate-limit.max-attempts=1000",
                // 화면이 보내는 요청마다 SQL 이 찍히면 실패 로그를 읽기 어려움
                "--logging.level.org.hibernate.SQL=info",
                "--logging.level.org.hibernate.orm.jdbc.bind=info",
                "--spring.jpa.show-sql=false"));
        all.addAll(Arrays.asList(args));

        SpringApplication.from(OdoLogApplication::main).run(all.toArray(String[]::new));
    }
}
