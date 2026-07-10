package com.tao.card_nav.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * langchain4j OpenAI Chat Model 配置属性。
 *
 * <p>实际 {@code ChatModel} Bean 由 langchain4j-spring-boot-starter 自动装配，
 * 本类仅用于绑定 yaml 中 {@code langchain4j.open-ai.chat-model.*} 配置到字段，
 * 供业务代码按需读取。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
public class AiModelConfig {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean logRequests = false;
    private Boolean logResponses = false;
    private Integer maxTokens;
    private Double temperature;
}
