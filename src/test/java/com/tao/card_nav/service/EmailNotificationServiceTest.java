package com.tao.card_nav.service;

import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EmailNotificationServiceTest {

    private JavaMailSender mailSender;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new EmailNotificationService(mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "bot@qq.com");
        ReflectionTestUtils.setField(service, "recipientEmail", "admin@qq.com");
    }

    @Test
    void sendDeleteCode_succeeds() {
        assertThatCode(() -> service.sendDeleteCode(42L, "123456"))
                .doesNotThrowAnyException();
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendDeleteCode_mailSendException_throwsBusinessException() {
        doThrow(new MailSendException("smtp failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.sendDeleteCode(42L, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.EMAIL_SEND_FAILED.getCode());
    }

    @Test
    void sendDeleteCode_subjectContainsCardNav() {
        service.sendDeleteCode(42L, "123456");

        org.mockito.ArgumentCaptor<SimpleMailMessage> captor =
                org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThatCode(() -> {
            SimpleMailMessage msg = captor.getValue();
            org.assertj.core.api.Assertions.assertThat(msg.getSubject()).contains("card-nav");
            org.assertj.core.api.Assertions.assertThat(msg.getText()).contains("123456").contains("42");
        }).doesNotThrowAnyException();
    }
}