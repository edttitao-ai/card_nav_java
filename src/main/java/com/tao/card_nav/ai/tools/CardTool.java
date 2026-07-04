package com.tao.card_nav.ai.tools;

import cn.hutool.json.JSONUtil;
import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.service.CardsService;
import com.tao.card_nav.service.CategoryService;
import com.tao.card_nav.service.SidebarService;
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
    private final CategoryService categoryService;
    private final SidebarService sidebarService;

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
    @Tool(name = "新增卡片", value = "创建新卡片，需要提供标题和URL，且分类和侧边栏必须确定（必填）。分类和侧边栏参数支持两种方式：1) 直接传ID（categoryId、sidebarId）；2) 传分类名称或侧边栏名称（categoryName、sidebarLabel），后端会自动查找对应ID。favicon 可不传，后端会从 URL 自动获取。返回创建成功的卡片信息")
    public String addCard(
            @P("卡片标题，不能为空") String title,
            @P("卡片URL，不能为空") String url,
            @P(value = "卡片描述，可为空", required = false) String description,
            @P(value = "卡片 favicon 图标URL，可为空（空则从 URL 自动获取）", required = false) String favicon,
            @P("分类ID（必填，与categoryName二选一）") Long categoryId,
            @P("侧边栏ID（必填，与sidebarLabel二选一）") String sidebarId,
            @P("分类名称（必填，与categoryId二选一，后端自动匹配ID）") String categoryName,
            @P("侧边栏名称（必填，与sidebarId二选一，后端自动匹配ID）") String sidebarLabel) {
        // 如果传入的是名称而非ID，自动匹配
        if (categoryId == null && categoryName != null && !categoryName.isEmpty()) {
            categoryId = matchCategoryIdByName(categoryName);
        }
        if ((sidebarId == null || sidebarId.isEmpty()) && sidebarLabel != null && !sidebarLabel.isEmpty()) {
            sidebarId = matchSidebarIdByLabel(sidebarLabel);
        }

        // 校验分类与侧边栏都已确定
        if (categoryId == null) {
            throw new BusinessException(400, "卡片必须指定分类（请提供 categoryId 或 categoryName）");
        }
        if (sidebarId == null || sidebarId.isEmpty()) {
            throw new BusinessException(400, "卡片必须指定侧边栏栏目（请提供 sidebarId 或 sidebarLabel）");
        }

        // favicon 自动兜底：未传则根据 URL 用 Google 服务拼一个
        if (favicon == null || favicon.isEmpty()) {
            favicon = buildFaviconFromUrl(url);
        }

        CardsDo card = CardsDo.builder()
                .title(title)
                .url(url)
                .description(description)
                .favicon(favicon)
                .categoryId(categoryId)
                .sidebarId(sidebarId)
                .build();
        CardsDo savedCard = cardsService.addCard(card);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("card", savedCard);
        result.put("resolvedCategoryId", categoryId);
        result.put("resolvedSidebarId", sidebarId);
        result.put("resolvedFavicon", favicon);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 根据 URL 使用 Google favicon 服务兜底生成图标地址
     */
    private String buildFaviconFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            String domain = new java.net.URL(url).getHost();
            if (domain == null || domain.isEmpty()) return null;
            return "https://www.google.com/s2/favicons?domain=" + domain + "&sz=32";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据分类名称匹配分类ID（精确优先，模糊兜底）
     */
    private Long matchCategoryIdByName(String name) {
        List<CategoryDo> categories = categoryService.getAllCategories();
        // 精确匹配
        for (CategoryDo c : categories) {
            if (c.getName() != null && c.getName().equals(name)) {
                return c.getId();
            }
        }
        // 包含匹配
        for (CategoryDo c : categories) {
            if (c.getName() != null && c.getName().contains(name)) {
                return c.getId();
            }
        }
        return null;
    }

    /**
     * 根据侧边栏名称匹配侧边栏ID（精确优先，模糊兜底）
     */
    private String matchSidebarIdByLabel(String label) {
        List<SidebarDo> sidebars = sidebarService.getAllSidebars();
        // 精确匹配
        for (SidebarDo s : sidebars) {
            if (s.getLabel() != null && s.getLabel().equals(label)) {
                return s.getId();
            }
        }
        // 包含匹配
        for (SidebarDo s : sidebars) {
            if (s.getLabel() != null && s.getLabel().contains(label)) {
                return s.getId();
            }
        }
        return null;
    }

    /**
     * 编辑卡片
     * 返回格式: {"success": true, "card": {...}}
     *
     * 所有非必填字段都为「传了才更新」语义；不传则保留原值。
     * 必传的是 cardId。
     * 如果改了分类/侧边栏相关的字段，传值方式和新增卡片一样：
     *   - 直接传 ID（categoryId / sidebarId）
     *   - 或者传名称（categoryName / sidebarLabel），后端自动匹配
     */
    @Tool(name = "编辑卡片", value = "修改已存在的卡片信息。必填：cardId。可选字段（不传则保留原值）：title、url、description、favicon、categoryId/sidebarId（或者对应的 categoryName/sidebarLabel）。修改链接或标题时会校验全表唯一冲突。返回更新后的卡片信息")
    public String updateCard(
            @P("卡片ID（必填）") Long cardId,
            @P(value = "新标题（可选，不传不改）", required = false) String title,
            @P(value = "新链接 URL（可选，不传不改）", required = false) String url,
            @P(value = "新描述（可选，不传不改）", required = false) String description,
            @P(value = "新 favicon URL（可选，不传不改）", required = false) String favicon,
            @P(value = "新分类ID（可选，与新 categoryName 二选一；都不传则不改分类）", required = false) Long categoryId,
            @P(value = "新侧边栏ID（可选，与新 sidebarLabel 二选一；都不传则不改侧边栏）", required = false) String sidebarId,
            @P(value = "新分类名称（可选，后端自动匹配ID）", required = false) String categoryName,
            @P(value = "新侧边栏名称（可选，后端自动匹配ID）", required = false) String sidebarLabel) {

        // 名称 → ID 自动匹配
        if (categoryId == null && categoryName != null && !categoryName.isEmpty()) {
            categoryId = matchCategoryIdByName(categoryName);
        }
        if ((sidebarId == null || sidebarId.isEmpty()) && sidebarLabel != null && !sidebarLabel.isEmpty()) {
            sidebarId = matchSidebarIdByLabel(sidebarLabel);
        }

        CardsDo update = new CardsDo();
        if (title != null && !title.isEmpty()) update.setTitle(title);
        if (url != null && !url.isEmpty()) update.setUrl(url);
        if (description != null && !description.isEmpty()) update.setDescription(description);
        if (favicon != null && !favicon.isEmpty()) update.setFavicon(favicon);
        if (categoryId != null) update.setCategoryId(categoryId);
        if (sidebarId != null && !sidebarId.isEmpty()) update.setSidebarId(sidebarId);

        // 全为空就相当于没改，直接返回原卡片
        if (update.getTitle() == null && update.getUrl() == null && update.getDescription() == null
                && update.getFavicon() == null && update.getCategoryId() == null
                && update.getSidebarId() == null) {
            CardsDo current = cardsService.getCardById(cardId);
            Map<String, Object> noop = new HashMap<>();
            noop.put("success", true);
            noop.put("card", current);
            noop.put("message", "未提供任何修改字段");
            return JSONUtil.toJsonStr(noop);
        }

        CardsDo saved = cardsService.updateCard(cardId, update);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("card", saved);
        result.put("resolvedCategoryId", categoryId);
        result.put("resolvedSidebarId", sidebarId);

        // 调试信息：让 AI 知道实际更新了哪些字段（如果传了 categoryName 但没匹配到，会是 null）
        Map<String, Object> resolvedFields = new HashMap<>();
        if (categoryName != null) {
            resolvedFields.put("requestedCategoryName", categoryName);
            resolvedFields.put("matchedCategoryId", categoryId);
        }
        if (sidebarLabel != null) {
            resolvedFields.put("requestedSidebarLabel", sidebarLabel);
            resolvedFields.put("matchedSidebarId", sidebarId);
        }
        if (!resolvedFields.isEmpty()) {
            result.put("debug", resolvedFields);
        }
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
