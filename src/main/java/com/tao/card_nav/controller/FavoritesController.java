package com.tao.card_nav.controller;

import com.tao.card_nav.entity.FavoritesDo;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.exception.ThrowUtils;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.FavoritesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final FavoritesService favoritesService;

    @GetMapping
    public Result<List<FavoritesDo>> getAll() {
        return Result.success(favoritesService.getAll());
    }

    @PostMapping
    public Result<Void> add(@RequestBody FavoritesDo favorite) {
        ThrowUtils.throwIf(favorite == null, ErrorCode.PARAMS_ERROR, "收藏对象不能为空");
        ThrowUtils.throwIf(favorite.getCardId() == null, ErrorCode.PARAMS_ERROR, "卡片ID不能为空");
        favoritesService.addFavorite(favorite);
        return Result.success();
    }

    @DeleteMapping("/{cardId}")
    public Result<Void> remove(@PathVariable Long cardId) {
        ThrowUtils.throwIf(cardId == null || cardId <= 0, ErrorCode.PARAMS_ERROR, "卡片ID不合法");
        favoritesService.removeFavorite(cardId);
        return Result.success();
    }
}