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

/**
 * {@link EmailNotificationService} 单测（mock {@link JavaMailSender}）。
 *
 * <p>测的核心契约：
 * <ul>
 *   <li>正常路径发邮件成功</li>
 *   <li>SMTP 抛 {@link MailSendException} → 抛 {@link BusinessException(EMAIL_SEND_FAILED)}</li>
 *   <li>邮件 subject 含 "card-nav"；正文含 cardId 与 code（确保用户能看到关键信息）</li>
 * </ul>
 */
class EmailNotificationServiceTest {

    private JavaMailSender mailSender;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new EmailNotificationService(mailSender);
        // 用 ReflectionTestUtils 注入 @Value 字段（单元测试不启动 Spring 上下文）
        ReflectionTestUtils.setField(service, "fromEmail", "bot@qq.com");
        ReflectionTestUtils.setField(service, "recipientEmail", "admin@qq.com");
    }

    /**
     * 正常路径：调一次 JavaMailSender.send，不抛异常。
     */
    @Test
    void sendDeleteCode_succeeds() {
        assertThatCode(() -> service.sendDeleteCode(42L, "123456"))
                .doesNotThrowAnyException();
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    /**
     * SMTP 故障（{@link MailSendException}）：service 必须转为业务异常
     * {@link ErrorCode#EMAIL_SEND_FAILED}，便于 GlobalExceptionHandler 返回 500
     * 而不是泄露 MailSendException 内部信息。
     */
    @Test
    void sendDeleteCode_mailSendException_throwsBusinessException() {
        doThrow(new MailSendException("smtp failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.sendDeleteCode(42L, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.EMAIL_SEND_FAILED.getCode());
    }

    /**
     * <b>内容契约</b>：邮件主题含 "card-nav"、正文含 cardId 与 code。
     * 防止"邮件发出去了但用户不知道是干嘛的"。
     */
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