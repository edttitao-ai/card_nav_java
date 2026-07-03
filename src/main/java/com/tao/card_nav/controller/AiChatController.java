package com.tao.card_nav.controller;


import com.tao.card_nav.ai.aiService.AiServiceAssistant;

import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    @Resource
    private AiServiceAssistant aiService;

    @PostMapping( produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody String userMessage) {
        return aiService.chatNav(userMessage);
    }
}
