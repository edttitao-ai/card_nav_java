package com.tao.card_nav.controller;

import com.tao.card_nav.domain.ClickWithCard;
import com.tao.card_nav.entity.ClicksDo;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.exception.ThrowUtils;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.ClicksService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clicks")
@RequiredArgsConstructor
public class ClicksController {

    private final ClicksService clicksService;

    /**
     * 点击卡片
     */
    @PostMapping
    public Result<Void> clickCard(@RequestBody ClickRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求对象不能为空");
        ThrowUtils.throwIf(request.getCardId() == null || request.getCardId().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "卡片ID不能为空");
        clicksService.clickCard(request.getCardId());
        return Result.success();
    }

    /**
     * 获取点击排行榜 TOP 10（联表返回卡片信息）
     */
    @GetMapping
    public Result<List<ClickWithCard>> getTopClicks() {
        return Result.success(clicksService.getTopClicks(10));
    }

    /**
     * 按 cardId 查询点击记录
     */
    @GetMapping("/{cardId}")
    public Result<ClicksDo> getClickByCardId(@PathVariable String cardId) {
        ThrowUtils.throwIf(cardId == null || cardId.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "卡片ID不能为空");
        return Result.success(clicksService.getClickByCardId(cardId));
    }

    /**
     * 点击请求对象
     */
    @lombok.Data
    public static class ClickRequest {
        private String cardId;
    }
}
