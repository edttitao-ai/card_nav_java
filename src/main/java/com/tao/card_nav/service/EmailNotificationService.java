package com.tao.card_nav.service;

import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件通知服务（QQ SMTP）。
 *
 * <p>当前仅承担"删除验证码通知"职责，发送地址来自
 * {@code card-nav.security.delete-recipient-email}（fail-fast 检测在 StartupConfig）。
 *
 * <p>注意：QQ 邮箱必须使用"授权码"而非登录密码（详见 README）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${card-nav.security.delete-recipient-email}")
    private String recipientEmail;

    /**
     * 发送 6 位删除验证码到指定收件箱。
     *
     * @param cardId 卡片 id（邮件正文提示）
     * @param code   6 位验证码
     * @throws BusinessException 邮件发送失败时抛 {@link ErrorCode#EMAIL_SEND_FAILED}
     */
    public void sendDeleteCode(Long cardId, String code) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(recipientEmail);
            msg.setSubject("[card-nav] 删除卡片验证码");
            msg.setText("""
                    卡片 ID: %d
                    验证码: %s
                    有效期: 5 分钟，一次性使用

                    请在 AI 对话窗口中把验证码原样发给 AI，
                    AI 拿到后会立即调用「确认删除卡片」工具完成软删除。
                    """.formatted(cardId, code));
            mailSender.send(msg);
            log.info("删除验证码邮件已发送: cardId={}, code={}", cardId, code);
        } catch (MailException e) {
            log.error("删除验证码邮件发送失败: cardId={}", cardId, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED,
                    "验证码邮件发送失败，请联系管理员检查 QQ 邮箱授权码");
        }
    }
}