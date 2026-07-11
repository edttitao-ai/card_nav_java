package com.tao.card_nav.ai.aiService;

import com.tao.card_nav.ai.tools.AiToolRegistry;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Service 装配工厂。
 *
 * <p>采用"显式构造器注入 + {@code @Qualifier} 按名绑定"：
 * <ul>
 *   <li>绕开字段注入反射改 {@code final} 的隐患（原 {@code @Resource(name=...)} + {@code private final}）。</li>
 *   <li>显式指定 {@code openAiChatModel} / {@code openAiStreamingChatModel} 这两个 langchain4j starter 暴露的 bean，
 *       为以后接入 ollama / 智谱等第二家 provider 时按名切换留口子。</li>
 *   <li>{@link AiToolRegistry} / {@link ChatMemoryProvider} 容器里唯一，按类型注入即可。</li>
 * </ul>
 */
@Configuration
public class AiServiceFactory {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final AiToolRegistry toolRegistry;
    private final ChatMemoryProvider chatMemoryProvider;

    public AiServiceFactory(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            @Qualifier("openAiStreamingChatModel") StreamingChatModel streamingChatModel,
            AiToolRegistry toolRegistry,
            ChatMemoryProvider chatMemoryProvider) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.toolRegistry = toolRegistry;
        this.chatMemoryProvider = chatMemoryProvider;
    }

    public AiServiceAssistant createAiServiceAssistant() {
        return AiServices.builder(AiServiceAssistant.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .tools(toolRegistry.activeTools().toArray())
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }

    @Bean
    public AiServiceAssistant aiServiceAssistant() {
        return createAiServiceAssistant();
    }
}
