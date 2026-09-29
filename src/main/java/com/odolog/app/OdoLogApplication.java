package com.odolog.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class OdoLogApplication {

    public static void main(String[] args) {
        // 기록 시각(createdAt 등)의 기준 시간대. "오늘" 판정은 사용자 시간대(UserToday)
        // run() 이전 호출 필수. 커넥션 풀·Hibernate 가 기동 시 시간대를 읽음
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        SpringApplication.run(OdoLogApplication.class, args);
    }
}
