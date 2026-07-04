package com.tao.card_nav.config.ai;


import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 对话记忆配置：按 memoryId（即 sessionId）隔离每个会话的历史消息
 *
 * 通过 AiServices.builder().chatMemoryProvider(...) 注入，
 * LangChain4j 会在调用 @MemoryId 标注的方法时按 memoryId 获取对应 memory。
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 全局 ChatMemory 存储（内存实现，重启会清空；可替换为 Redis / JDBC 持久化）
     */
    @Bean
    public InMemoryChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    /**
     * 提供者：按 memoryId 返回对应的 ChatMemory 实例，
     * 每个会话保留最近 50 条消息，超过会丢掉最早的消息。
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(InMemoryChatMemoryStore store) {
        Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
        return memoryId -> memories.computeIfAbsent(memoryId, k ->
                MessageWindowChatMemory.builder()
                        .id(k)
                        .maxMessages(50)
                        .chatMemoryStore(store)
                        .build()
        );
    }
}
