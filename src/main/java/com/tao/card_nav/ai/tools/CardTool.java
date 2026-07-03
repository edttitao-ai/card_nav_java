package com.tao.card_nav.ai.tools;

import cn.hutool.json.JSONUtil;
import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.service.CardsService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CardTool {

    private final CardsService cardsService;

    /**
     * 根据卡片ID查询卡片详情
     * 返回格式: {"id": 1, "title": "卡片标题", "url": "https://...", "description": "描述", "category": "分类名", "pinned": false}
     */
    @Tool(name = "查询卡片", value = "根据卡片ID查询卡片详情，返回卡片的完整信息包括标题、URL、描述、分类等")
    public String getCardById(@P("卡片ID") Long id) {
        CardsDo card = cardsService.getCardById(id);
        return JSONUtil.toJsonStr(card);
    }

    /**
     * 查询卡片列表
     * 返回格式: [{"id": 1, "title": "卡片标题", "url": "https://...", ...}, ...]
     */
    @Tool(name = "查询卡片列表", value = "查询所有卡片列表，或按侧边栏ID过滤。返回卡片数组，每个卡片包含id、title、url、description、category、pinned等字段")
    public String getCards(@P(value = "侧边栏ID，可为空", required = false) String sidebarId) {
        List<CardsDo> cards = cardsService.getCards(sidebarId);
        return JSONUtil.toJsonStr(cards);
    }

    /**
     * 新增卡片
     * 返回格式: {"success": true, "card": {"id": 1, "title": "卡片标题", ...}}
     */
    @Tool(name = "新增卡片", value = "创建新卡片，需要提供标题和URL，可选提供描述、分类ID、侧边栏ID。返回创建成功的卡片信息")
    public String addCard(
            @P("卡片标题，不能为空") String title,
            @P("卡片URL，不能为空") String url,
            @P(value = "卡片描述，可为空", required = false) String description,
            @P(value = "分类ID，可为空", required = false) Long categoryId,
            @P(value = "侧边栏ID，可为空", required = false) String sidebarId) {
        CardsDo card = CardsDo.builder()
                .title(title)
                .url(url)
                .description(description)
                .categoryId(categoryId)
                .sidebarId(sidebarId)
                .build();
        CardsDo savedCard = cardsService.addCard(card);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("card", savedCard);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 切换卡片置顶状态
     * 返回格式: {"success": true, "card": {"id": 1, "title": "卡片标题", "pinned": true}}
     */
    @Tool(name = "切换置顶状态", value = "设置卡片的置顶状态，pinned为true表示置顶，false表示取消置顶")
    public String togglePinned(@P("卡片ID") Long id, @P("是否置顶，true表示置顶，false表示取消置顶") Boolean pinned) {
        CardsDo card = cardsService.togglePinned(id, pinned);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("card", card);
        return JSONUtil.toJsonStr(result);
    }
}
