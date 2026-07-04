package com.tao.card_nav.ai.aiService;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;


public interface AiServiceAssistant {

    @SystemMessage(fromResource = "prompt/temp-system-prompt.txt")
    Flux<String> chatNav(@MemoryId String sessionId, @UserMessage String userMessage);

}
