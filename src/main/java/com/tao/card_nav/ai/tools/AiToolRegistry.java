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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 工具注册表（Registry）。
 *
 * <p>职责：
 * <ul>
 *   <li>启动时自动收集所有 {@link AiToolProvider} Bean。</li>
 *   <li>聚合各家提供的 LangChain4j Tool 对象，缓存供 Factory 装配 Assistant。</li>
 *   <li>启动期校验所有 {@code @Tool(name)} 唯一，冲突立即失败（避免 LLM 选错工具）。</li>
 *   <li>支持运行时 {@link #disable(String)} / {@link #enable(String)} 切换（为未来按角色过滤 Tool 铺路）。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AiToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiToolRegistry.class);

    private final List<AiToolProvider> providers;

    /** 被禁用的 provider key 集合（线程安全） */
    private final Set<String> disabled = ConcurrentHashMap.newKeySet();

    /** 当前生效的所有 Tool 对象（不可变快照） */
    private volatile List<Object> cachedTools = List.of();

    /** 当前生效的所有 @Tool 名称（用于诊断） */
    private volatile Set<String> cachedToolNames = Set.of();

    @PostConstruct
    void init() {
        rebuild();
        log.info("AiToolRegistry 初始化: providers={}, activeTools={}, toolNames={}",
                providers.size(), cachedTools.size(), cachedToolNames);
    }

    /** 当前生效的所有 Tool 对象 */
    public List<Object> activeTools() {
        return cachedTools;
    }

    /** 当前生效的所有 Tool 名称（用于诊断/调试） */
    public Set<String> toolNames() {
        return cachedToolNames;
    }

    /** 运行时禁用某个 provider（按 {@link AiToolProvider#providerKey()}） */
    public void disable(String providerKey) {
        if (disabled.add(providerKey)) {
            rebuild();
            log.warn("AI Tool 已禁用: provider={}, activeTools={}", providerKey, cachedToolNames);
        }
    }

    /** 运行时启用某个 provider */
    public void enable(String providerKey) {
        if (disabled.remove(providerKey)) {
            rebuild();
            log.info("AI Tool 已启用: provider={}, activeTools={}", providerKey, cachedToolNames);
        }
    }

    /** 是否处于禁用状态 */
    public boolean isDisabled(String providerKey) {
        return disabled.contains(providerKey);
    }

    private void rebuild() {
        List<Object> all = providers.stream()
                .filter(p -> !disabled.contains(p.providerKey()))
                .flatMap(p -> {
                    List<Object> ts = p.tools();
                    return ts == null ? java.util.stream.Stream.empty() : ts.stream();
                })
                .collect(Collectors.toUnmodifiableList());

        Set<String> names = extractToolNames(all);

        this.cachedTools = all;
        this.cachedToolNames = names;
    }

    /**
     * 提取所有 {@code @Tool(name)}，并校验唯一性。
     * 重复 → 启动期 {@link IllegalStateException}，避免运行时 LLM 行为诡异。
     */
    private Set<String> extractToolNames(List<Object> tools) {
        Set<String> names = new HashSet<>();
        for (Object toolObj : tools) {
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
        return Set.copyOf(names);
    }
}