package com.tao.card_nav.config.ai;



import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


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

//    @Bean
//    public ChatModel aiChatModel() {
//        return OpenAiChatModel.builder()
//                .apiKey(apiKey)
//                .modelName(modelName)
//                .baseUrl(baseUrl)
//                .maxTokens(maxTokens)
//                .temperature(temperature)
//                .logRequests(logRequests)
//                .logResponses(logResponses)
//                .build();
//    }
}
