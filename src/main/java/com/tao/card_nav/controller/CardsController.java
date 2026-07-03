package com.tao.card_nav.controller;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.exception.ThrowUtils;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.CardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardsController {

    private final CardsService cardsService;

    /**
     * 查询所有卡片，支持按 sidebarId 过滤
     */
    @GetMapping
    public Result<List<CardsDo>> getCards(@RequestParam(required = false) String sidebarId) {
        return Result.success(cardsService.getCards(sidebarId));
    }

    /**
     * 按 ID 查询卡片
     */
    @GetMapping("/{id}")
    public Result<CardsDo> getCardById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "卡片ID不合法");
        return Result.success(cardsService.getCardById(id));
    }

    /**
     * 新增卡片
     */
    @PostMapping
    public Result<CardsDo> addCard(@RequestBody CardsDo card) {
        ThrowUtils.throwIf(card == null, ErrorCode.PARAMS_ERROR, "卡片对象不能为空");
        ThrowUtils.throwIf(card.getTitle() == null || card.getTitle().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "卡片标题不能为空");
        return Result.success(cardsService.addCard(card));
    }

    /**
     * 更新卡片
     */
    @PutMapping("/{id}")
    public Result<CardsDo> updateCard(@PathVariable Long id, @RequestBody CardsDo card) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "卡片ID不合法");
        ThrowUtils.throwIf(card == null, ErrorCode.PARAMS_ERROR, "卡片对象不能为空");
        return Result.success(cardsService.updateCard(id, card));
    }

    /**
     * 删除卡片（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteCard(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "卡片ID不合法");
        cardsService.deleteCard(id);
        return Result.success();
    }

    /**
     * 切换置顶状态
     */
    @PutMapping("/{id}/pinned")
    public Result<CardsDo> togglePinned(@PathVariable Long id, @RequestParam Boolean pinned) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "卡片ID不合法");
        ThrowUtils.throwIf(pinned == null, ErrorCode.PARAMS_ERROR, "置顶状态不能为空");
        return Result.success(cardsService.togglePinned(id, pinned));
    }
}
