package com.odolog.app.user.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 재설정 링크 발송. 링크는 프런트 주소
 * 커밋 후 다른 스레드에서 발송. 응답 시간 차이 방지, 롤백 시 미발송
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final JavaMailSender mailSender;
    private final TaskExecutor taskExecutor;
    private final String baseUrl;
    private final String from;

    public PasswordResetMailer(JavaMailSender mailSender,
                               TaskExecutor taskExecutor,
                               @Value("${odolog.app.base-url}") String baseUrl,
                               @Value("${spring.mail.username:}") String from) {
        this.mailSender = mailSender;
        this.taskExecutor = taskExecutor;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    /** 즉시 반환. 실패는 로그로만 */
    public void send(String email, String token, int validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        // from 이 비면 spring.mail.username 사용
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

        Runnable delivery = () -> deliver(message);

        // 트랜잭션 안이면 커밋 후 발송
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(delivery);
                }
            });
        } else {
            taskExecutor.execute(delivery);
        }
    }

    private void deliver(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            // 메일 설정 문제는 운영 쪽 확인 사항
            log.error("비밀번호 재설정 메일 발송 실패. 메일 설정을 확인하세요.", e);
        }
    }
}
