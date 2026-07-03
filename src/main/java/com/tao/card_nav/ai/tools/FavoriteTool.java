package com.tao.card_nav.ai.tools;

import cn.hutool.json.JSONUtil;
import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.entity.FavoritesDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.service.CardsService;
import com.tao.card_nav.service.FavoritesService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FavoriteTool {

    private final FavoritesService favoritesService;

    private final CardsService cardsService;

    /**
     * 查询所有收藏
     * 返回格式: [{"id": 1, "cardId": 1, "title": "卡片标题", "url": "https://...", ...}, ...]
     */
    @Tool(name = "查询收藏列表", value = "查询用户的所有收藏列表，返回收藏卡片的信息包括卡片ID、标题、URL、描述等")
    public String getAllFavorites() {
        List<FavoritesDo> favorites = favoritesService.getAll();
        return JSONUtil.toJsonStr(favorites);
    }

    /**
     * 添加收藏
     * 返回格式: {"success": true, "favorite": {"id": 1, "cardId": 1, ...}}
     */
    @Tool(name = "添加收藏", value = "将指定卡片添加到收藏列表，需要提供卡片ID")
    public String addFavorite(@P("卡片ID") Long cardId) {
        // 先根据 cardId 查询卡片完整信息
        CardsDo card = cardsService.getCardById(cardId);
        if (card == null) {
            throw new BusinessException("卡片不存在，ID: " + cardId);
        }

        FavoritesDo favorite = FavoritesDo.builder()
                .cardId(card.getId())
                .title(card.getTitle())
                .url(card.getUrl())
                .description(card.getDescription())
                .category(card.getCategory())
                .favicon(card.getFavicon())
                .build();
        favoritesService.addFavorite(favorite);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("favorite", favorite);
        return JSONUtil.toJsonStr(result);
    }

    /**
     * 移除收藏
     * 返回格式: {"success": true, "message": "已取消收藏"}
     */
    @Tool(name = "移除收藏", value = "从收藏列表中移除指定卡片，需要提供卡片ID")
    public String removeFavorite(@P("卡片ID") Long cardId) {
        favoritesService.removeFavorite(cardId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已取消收藏");
        return JSONUtil.toJsonStr(result);
    }
}
