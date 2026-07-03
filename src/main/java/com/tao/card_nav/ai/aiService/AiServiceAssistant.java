package com.tao.card_nav.ai.aiService;

import dev.langchain4j.service.SystemMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;


public interface AiServiceAssistant {

    @SystemMessage(fromResource = "prompt/temp-system-prompt.txt")
    Flux<String> chatNav(String userMessage);

}
