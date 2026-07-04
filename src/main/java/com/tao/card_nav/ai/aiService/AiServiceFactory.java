package com.tao.card_nav.ai.aiService;

import com.tao.card_nav.ai.tools.CardTool;
import com.tao.card_nav.ai.tools.CategoryTool;
import com.tao.card_nav.ai.tools.FavoriteTool;
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

    private final CardTool cardTool;

    private final FavoriteTool favoriteTool;

    private final CategoryTool categoryTool;

    public AiServiceAssistant createAiServiceAssistant() {
        return AiServices.builder(AiServiceAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .tools(cardTool, favoriteTool, categoryTool)
                .build();

    }

    @Bean
    public AiServiceAssistant aiServiceAssistant() {
        return createAiServiceAssistant();
    }
}
