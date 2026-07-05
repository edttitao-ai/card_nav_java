package com.tao.card_nav.ai.tools;

import java.util.List;

/**
 * AI 工具策略接口（Strategy）。
 *
 * <p>实现类必须是 Spring 组件（{@code @Component}），由 {@link AiToolRegistry} 自动收集。
 * <p>新增 Tool 只需新增一个实现类，不再需要改 Factory。
 */
public interface AiToolProvider {

    /**
     * 当前策略提供的 LangChain4j Tool 对象列表。
     * <ul>
     *   <li>单数场景：直接 {@code return List.of(this);}，{@code @Tool} 注解方法由 LangChain4j 反射扫描。</li>
     *   <li>组合场景：可返回多个聚合对象。</li>
     * </ul>
     */
    List<Object> tools();

    /**
     * 提供者键。disable/enable 时按此键匹配。
     * 默认取实现类 SimpleName；如需稳定的"业务名"（不与类名耦合），可 override。
     */
    default String providerKey() {
        return getClass().getSimpleName();
    }
}