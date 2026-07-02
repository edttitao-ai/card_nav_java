package com.tao.card_nav.controller;

import com.tao.card_nav.domain.CardLogWithCard;
import com.tao.card_nav.entity.CardLogsDo;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.CardLogsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class CardLogsController {

    private final CardLogsService cardLogsService;

    /**
     * 查询所有操作日志（带卡片标题、栏目、分类）
     */
    @GetMapping
    public Result<List<CardLogWithCard>> getAllLogs() {
        return Result.success(cardLogsService.getAllLogs());
    }

    /**
     * 查询某个卡片的所有操作日志
     */
    @GetMapping("/{cardId}")
    public Result<List<CardLogsDo>> getLogsByCardId(@PathVariable Long cardId) {
        return Result.success(cardLogsService.getLogsByCardId(cardId));
    }
}
