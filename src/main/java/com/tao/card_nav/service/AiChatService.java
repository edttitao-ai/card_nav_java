package com.tao.card_nav.service;

import com.tao.card_nav.ai.aiService.AiServiceAssistant;
import com.tao.card_nav.ai.aiService.AiServiceFactory;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiChatService {

    @Resource
    private AiServiceFactory aiServiceFactory;

    public Flux<String> aiChat(String userMessage){
        AiServiceAssistant aiServiceAssistant = aiServiceFactory.createAiServiceAssistant();
        return aiServiceAssistant.chatNav(userMessage);
    }
}
