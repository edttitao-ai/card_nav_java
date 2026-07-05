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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CardTool implements AiToolProvider {

    @Override
    public List<Object> tools() {
        return List.of(this);
    }

    private final CardsService cardsService;
    private final CategoryService categoryService;
    private final SidebarService sidebarService;

    /**
     * AI 删除卡片所需的"确认指令"。
     * <p>读取 application.yml 里的 {@code card-nav.security.delete-confirmation}，
     * 其值是占位符 {@code ${CARD_NAV_SECURITY_DELETE_CONFIRMATION}}，运行时由 dotenv 加载的 .env 注入。
     * <p>严格模式：未配置环境变量时应用启动失败（fail-fast），不会用任何占位符兜底。
     * <p>校验规则：字面精确 equals（不 trim、区分大小写），前后空格会导致校验失败。
     */
    @Value("${card-nav.security.delete-confirmation}")
    private String expectedConfirmation;

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
     * 查询卡片列表（AI 场景，硬上限 20 条，字段已裁剪）
     * 返回格式: {"items": [{id, title, url, category, pinned, sidebarId}, ...], "tip": "..."}
     * 当结果可能不完整时，tip 字段会提示使用「搜索卡片」工具按关键词召回更精准的结果
     */
    @Tool(name = "查询卡片列表", value = "查询所有卡片或按侧边栏ID过滤。一次最多返回 20 条卡片摘要（不含 description），按置顶优先排序。如果用户描述模糊（如「我收藏的 AI 工具」），应优先用「搜索卡片」工具按关键词召回。本工具仅在用户明确要全量/列表时使用")
    public String getCards(@P(value = "侧边栏ID，可为空", required = false) String sidebarId) {
        final int LIMIT = 20;
        List<CardsDo> cards = cardsService.getCardsLimited(sidebarId, LIMIT);
        List<Map<String, Object>> items = cards.stream().map(this::toAiListItem).toList();
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("count", items.size());
        result.put("limit", LIMIT);
        result.put("truncated", items.size() >= LIMIT);
        result.put("tip", "结果最多 20 条，可能不完整。如需精准召回请使用「搜索卡片」工具传 keyword 参数");
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 搜索卡片（AI 场景，按关键词 + 侧边栏 + 分类检索）
     * 返回格式: {"items": [{id, title, category, sidebarLabel, snippet, pinned}], "count": N, "limit": 20}
     * keyword 匹配 title/description/url；其他过滤条件为空则不参与过滤
     */
    @Tool(name = "搜索卡片", value = "按关键词检索卡片，同时可按侧边栏ID/分类ID过滤。keyword 会模糊匹配卡片的标题、描述、URL。一次最多返回 20 条摘要，包含 id/title/category/sidebarLabel/snippet(描述前80字)/pinned。这是用户表达模糊需求时（如「找 AI 写代码的工具」「我那个 GitHub 链接」）的默认首选工具")
    public String searchCards(
            @P(value = "搜索关键词（可选），匹配标题/描述/URL", required = false) String keyword,
            @P(value = "侧边栏ID（可选），与 keyword 叠加过滤", required = false) String sidebarId,
            @P(value = "分类ID（可选），与 keyword 叠加过滤", required = false) Long categoryId) {
        final int LIMIT = 20;
        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        List<CardsDo> cards = cardsService.searchCards(kw, sidebarId, categoryId, LIMIT);

        // 顺便把侧边栏 label 拿到，做 sidebarLabel 字段
        java.util.Map<String, String> sidebarLabelMap = new java.util.HashMap<>();
        try {
            for (SidebarDo s : sidebarService.getAllSidebars()) {
                sidebarLabelMap.put(s.getId(), s.getLabel());
            }
        } catch (Exception ignore) {
        }

        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (CardsDo c : cards) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("category", c.getCategory());
            item.put("sidebarId", c.getSidebarId());
            item.put("sidebarLabel", sidebarLabelMap.getOrDefault(c.getSidebarId(), c.getSidebarId()));
            item.put("pinned", c.getPinned());
            String desc = c.getDescription();
            item.put("snippet", desc == null || desc.isEmpty() ? "" :
                    (desc.length() > 80 ? desc.substring(0, 80) + "..." : desc));
            items.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("count", items.size());
        result.put("limit", LIMIT);
        result.put("truncated", items.size() >= LIMIT);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 把卡片压缩为列表项（不含 description，节省 token）
     */
    private Map<String, Object> toAiListItem(CardsDo c) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", c.getId());
        item.put("title", c.getTitle());
        item.put("url", c.getUrl());
        item.put("category", c.getCategory());
        item.put("sidebarId", c.getSidebarId());
        item.put("pinned", c.getPinned());
        return item;
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

    /**
     * 删除卡片（AI 场景，需要确认指令）
     * <p>校验规则：
     * <ul>
     *   <li>confirmation 必须与 {@code expectedConfirmation} 字面精确 equals（不 trim、区分大小写）</li>
     *   <li>校验失败抛 BusinessException，提示模糊，不暴露密码长度/字符集</li>
     *   <li>校验通过后调用 cardsService.deleteCard 走软删除 + 发布 CardChangedEvent</li>
     * </ul>
     */
    @Tool(name = "删除卡片", value = "删除指定卡片（软删除）。这是一个高危操作，调用前必须先通过「查询卡片」或「搜索卡片」核对要删除的卡片信息。调用本工具时，confirmation 参数必须传入用户在自己上一条消息里原文提供的专属密码串（由系统后端按字面精确 equals 校验，不做任何模糊匹配或猜测），不要让用户输入任何你自创的口令（如'确认删除','yes'等），如未提供正确的确认指令，必须拒绝执行")
    public String deleteCard(@P("卡片ID") Long cardId, @P("用户原文提供的专属密码串，由系统按字面精确匹配校验") String confirmation) {
        if (cardId == null || cardId <= 0) {
            throw new BusinessException(400, "卡片ID不合法");
        }
        // 字面精确匹配：不 trim、不 lowercase、不忽略 null
        if (confirmation == null || !confirmation.equals(expectedConfirmation)) {
            // 模糊提示：不透露密码长度、字符集、是否包含空格等任何线索
            throw new BusinessException(403, "确认指令不匹配，请用户提供完整且准确的确认指令后再试");
        }
        // 校验通过：先确认卡片存在（deleteCard 内部已校验，这里加一次是给 AI 更明确的反馈）
        CardsDo existing = cardsService.getCardById(cardId);
        cardsService.deleteCard(cardId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("deletedCardId", existing.getId());
        result.put("deletedTitle", existing.getTitle());
        return JSONUtil.toJsonStr(result);
    }
}
