package com.tao.card_nav.controller;


import cn.hutool.json.JSONUtil;
import com.tao.card_nav.ai.aiService.AiServiceAssistant;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.exception.ThrowUtils;

import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    @Resource
    private AiServiceAssistant aiService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("userMessage");
        String sessionId = payload.get("sessionId");
        ThrowUtils.throwIf(userMessage == null || userMessage.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 若前端没传 sessionId，后端自动生成一个，避免 memory 维度混乱
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "anon-" + UUID.randomUUID().toString();
        }
        return aiService.chatNav(sessionId, userMessage)
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件（data 为空字符串，前端按 [DONE] 兼容）
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }
}
