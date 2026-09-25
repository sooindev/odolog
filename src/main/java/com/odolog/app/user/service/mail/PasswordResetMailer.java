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
 * 재설정 링크 발송
 *
 * 링크는 백엔드가 아니라 **프런트 주소**를 가리킨다 — 토큰을 받아 새 비밀번호를 입력받는 것은
 * 화면의 일이고, 백엔드는 그 화면이 보내 주는 값을 검증할 뿐이다
 *
 * 발송은 커밋 뒤, 다른 스레드에서. 요청 스레드에서 보내면 가입된 주소만 SMTP 시간만큼 늦게 답해
 * 응답 시간이 가입 여부를 알려준다. 커밋 전에 보내면 저장 안 된 토큰이 메일로 나갈 수 있다
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

    /** 곧바로 돌아온다. 실패는 로그로만 — 부른 쪽이 알 방법이 없어야 응답이 갈리지 않는다 */
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

        Runnable delivery = () -> deliver(message);

        // 트랜잭션 안이면 커밋이 끝난 뒤에. 롤백되면 아예 안 나간다
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
            // 메일 설정이 잘못된 것은 운영 쪽 문제. 요청한 사람이 알아서 할 수 있는 일이 아니다
            log.error("비밀번호 재설정 메일 발송 실패. 메일 설정을 확인하세요.", e);
        }
    }
}
