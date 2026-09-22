package com.odolog.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class OdoLogApplication {

    public static void main(String[] args) {
        /*
         * 앱의 시간대를 한국으로 고정한다
         *
         * 이 앱의 날짜는 전부 "사람이 쓰는 달력 날짜"다 — 주유한 날, 정비한 날.
         * 그런데 @PastOrPresent 와 LocalDate.now() 는 JVM 기본 시간대를 따르므로,
         * 서버가 UTC 로 도는 환경이면 한국 사용자가 고른 "오늘" 이 아직 미래라서
         * **매일 오전 9시까지 400 이 난다.** 홈 요약의 월별 12칸도 같은 이유로 하루씩 밀린다.
         *
         * 프런트는 이미 todayString() 으로 로컬 기준을 쓰고 있어, 여기를 고정해야 양쪽이 맞는다.
         *
         * SpringApplication.run 보다 먼저 부르는 이유: 커넥션 풀과 Hibernate 가 뜰 때
         * 기본 시간대를 한 번 읽어 가서, 그 뒤에 바꾸면 이미 늦는다.
         */
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        SpringApplication.run(OdoLogApplication.class, args);
    }
}
