package com.tao.card_nav.ai.aiService;

import com.tao.card_nav.ai.tools.CardTool;
import com.tao.card_nav.ai.tools.FavoriteTool;
import com.tao.card_nav.service.CardsService;
import com.tao.card_nav.service.FavoritesService;
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
    @Resource
    private final CardsService cardsService;
    @Resource
    private final FavoritesService favoritesService;

    public AiServiceAssistant createAiServiceAssistant() {
        // 创建 Tool 实例
        CardTool cardTool = new CardTool(cardsService);
        FavoriteTool favoriteTool = new FavoriteTool(favoritesService);

        return AiServices.builder(AiServiceAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .tools(cardTool, favoriteTool)
                .build();

    }

    @Bean
    public AiServiceAssistant aiServiceAssistant() {
        return createAiServiceAssistant();
    }
}
