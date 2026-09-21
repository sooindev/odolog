package com.odolog.app.user.service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 재설정 링크 발송
 *
 * 링크는 백엔드가 아니라 **프런트 주소**를 가리킨다 — 토큰을 받아 새 비밀번호를 입력받는 것은
 * 화면의 일이고, 백엔드는 그 화면이 보내 주는 값을 검증할 뿐이다
 */
@Component
public class PasswordResetMailer {

    private final JavaMailSender mailSender;
    private final String baseUrl;
    private final String from;

    public PasswordResetMailer(JavaMailSender mailSender,
                               @Value("${odolog.app.base-url}") String baseUrl,
                               @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    public void send(String email, String token, int validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        // from 이 비어 있으면 스프링이 spring.mail.username 을 쓴다. 둘 다 없으면 발송만 실패한다
        if (!from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject("[오도로그] 비밀번호 재설정");
        message.setText("""
                아래 링크에서 새 비밀번호를 설정하세요.

                %s/reset-password?token=%s

                이 링크는 %d분 동안만 쓸 수 있고, 한 번 쓰면 사라집니다.
                본인이 요청한 것이 아니라면 이 메일을 무시하세요. 비밀번호는 그대로입니다.
                """.formatted(baseUrl, URLEncoder.encode(token, StandardCharsets.UTF_8), validMinutes));

        mailSender.send(message);
    }
}
