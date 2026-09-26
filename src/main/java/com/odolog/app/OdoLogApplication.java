package com.odolog.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class OdoLogApplication {

    public static void main(String[] args) {
        // 앱 시간대를 Asia/Seoul 로 고정. UTC 서버에서 한국의 오늘이 미래로 판정되는 문제 방지
        // run() 이전 호출 필수. 커넥션 풀·Hibernate 가 기동 시 시간대를 읽음
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        SpringApplication.run(OdoLogApplication.class, args);
    }
}
