package com.tao.card_nav.ai.aiService;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class AiServiceFactory {


    @Resource(name = "openAiChatModel")
    private final ChatModel chatModel;

    @Resource(name = "openAiStreamingChatModel")
    private final StreamingChatModel streamingChatModel;

    public AiServiceAssistant createAiServiceAssistant() {
        return AiServices.builder(AiServiceAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();

    }

    @Bean
    public AiServiceAssistant aiServiceAssistant() {
        return createAiServiceAssistant();
    }
}
