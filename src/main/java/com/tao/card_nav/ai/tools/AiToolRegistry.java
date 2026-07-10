package com.tao.card_nav.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AI 工具注册表。
 *
 * <p>职责：
 * <ul>
 *   <li>启动期收集所有 {@link AiToolProvider} Bean。</li>
 *   <li>聚合各家提供的 LangChain4j Tool 对象，缓存供 Factory 装配 Assistant。</li>
 *   <li>启动期校验所有 {@code @Tool(name)} 唯一，冲突立即失败（避免 LLM 选错工具）。</li>
 * </ul>
 *
 * <p>历史上曾包含运行时 {@code disable/enable} 能力（全工程零调用方），已删除。
 * 如未来需要按角色过滤 Tool，请用 {@code @ConditionalOnProperty} 或独立 {@code AiToolProvider} 接口实现，而不是再回到运行时 toggle。
 */
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiToolRegistry.class);

    private final List<AiToolProvider> providers;

    private List<Object> cachedTools;

    @PostConstruct
    void init() {
        List<Object> all = providers.stream()
                .flatMap(p -> {
                    List<Object> ts = p.tools();
                    return ts == null ? Stream.empty() : ts.stream();
                })
                .collect(Collectors.toUnmodifiableList());

        // 启动期校验 @Tool(name) 唯一。重复 → IllegalStateException，立即失败。
        Set<String> names = new HashSet<>();
        for (Object toolObj : all) {
            for (Method m : toolObj.getClass().getMethods()) {
                Tool ann = m.getAnnotation(Tool.class);
                if (ann == null) {
                    continue;
                }
                String name = ann.name();
                if (name == null || name.isEmpty()) {
                    throw new IllegalStateException(
                            "@Tool 注解缺少 name: " + toolObj.getClass().getSimpleName() + "#" + m.getName());
                }
                if (!names.add(name)) {
                    throw new IllegalStateException("@Tool 名字重复: " + name
                            + "（请检查 " + toolObj.getClass().getSimpleName() + "#" + m.getName() + "）");
                }
            }
        }
        this.cachedTools = List.copyOf(all);
        log.info("AiToolRegistry 初始化完成: providers={}, activeTools={}, toolNames={}",
                providers.size(), cachedTools.size(), names);
    }

    /**
     * 当前生效的所有 Tool 对象（不可变快照）。
     */
    public List<Object> activeTools() {
        return cachedTools;
    }
}
