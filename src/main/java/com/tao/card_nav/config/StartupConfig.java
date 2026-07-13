package com.tao.card_nav.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 启动期 fail-fast 校验：检测关键配置未填时立刻失败，避免运行期才发现。
 *
 * <p>当前检查项：
 * <ul>
 *   <li>{@code card-nav.security.delete-recipient-email}：AI 删除的 QQ 收件箱</li>
 *   <li>{@code spring.mail.username / password}：QQ SMTP 登录凭据（授权码）</li>
 * </ul>
 */
@Slf4j
@Configuration
public class StartupConfig {

    private static final String NOT_SET = "NOT_SET";

    @Value("${card-nav.security.delete-recipient-email:NOT_SET}")
    private String deleteRecipientEmail;

    @Value("${spring.mail.username:NOT_SET}")
    private String mailUsername;

    @Value("${spring.mail.password:NOT_SET}")
    private String mailPassword;

    @PostConstruct
    void validateRequiredConfig() {
        log.info("StartupConfig 校验关键配置...");

        if (NOT_SET.equals(deleteRecipientEmail) || deleteRecipientEmail.isBlank()) {
            throw new IllegalStateException(
                    "未配置 card-nav.security.delete-recipient-email（AI 删除的 QQ 收件箱）。"
                            + "请设置环境变量 CARD_NAV_DELETE_RECIPIENT 或 application*.yml。");
        }
        if (NOT_SET.equals(mailUsername) || mailUsername.isBlank()) {
            throw new IllegalStateException(
                    "未配置 spring.mail.username（QQ 邮箱）。"
                            + "请设置环境变量 QQ_MAIL。");
        }
        if (NOT_SET.equals(mailPassword) || mailPassword.isBlank()) {
            throw new IllegalStateException(
                    "未配置 spring.mail.password（QQ 邮箱授权码）。"
                            + "请设置环境变量 QQ_AUTH_CODE。"
                            + "申请方式：QQ 邮箱 → 设置 → 账户 → POP3/IMAP/SMTP/Exchange/CardDAV/CalDAV服务。");
        }
        log.info("StartupConfig 校验通过：delete-recipient={}, mail-username={}",
                maskEmail(deleteRecipientEmail), mailUsername);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}