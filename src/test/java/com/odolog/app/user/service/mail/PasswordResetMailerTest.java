package com.odolog.app.user.service.mail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** 발송 시점과 스레드. 요청 스레드에서 보내면 응답 시간 차이 발생 */
class PasswordResetMailerTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    // 실행 대기열만 수집. 요청 스레드 미발송 확인용
    private final List<Runnable> queued = new ArrayList<>();
    private final PasswordResetMailer mailer =
            new PasswordResetMailer(mailSender, queued::add, "http://localhost:5173", "");

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("요청 스레드에서 보내지 않고 실행기에 넘긴다")
    void handsOffToExecutor() {
        mailer.send("me@odolog.com", "token", 30);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(queued).hasSize(1);

        queued.get(0).run();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("트랜잭션 안이면 커밋 뒤에야 넘긴다")
    void waitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();

        mailer.send("me@odolog.com", "token", 30);
        assertThat(queued).isEmpty();

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(queued).hasSize(1);
    }

    @Test
    @DisplayName("발송이 실패해도 예외가 밖으로 나가지 않는다")
    void swallowsDeliveryFailure() {
        doThrow(new MailSendException("SMTP 실패")).when(mailSender).send(any(SimpleMailMessage.class));

        mailer.send("me@odolog.com", "token", 30);

        assertThatCode(() -> queued.get(0).run()).doesNotThrowAnyException();
    }
}
