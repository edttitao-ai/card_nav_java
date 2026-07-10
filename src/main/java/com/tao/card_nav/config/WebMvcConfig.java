package com.tao.card_nav.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 占位：当前没有任何拦截器注册。
 *
 * <p>原 {@code VisitorInterceptor} 仅 {@code return true}，是 noop，已删除。
 * 若未来需要"请求级横切"（日志、限流、访问统计），请新建具备真实逻辑的
 * {@link org.springframework.web.servlet.HandlerInterceptor} 并在此处显式注册。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
