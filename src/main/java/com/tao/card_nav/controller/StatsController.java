package com.tao.card_nav.controller;

import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 获取统计数据（从 SQL 实时汇总）
     */
    @GetMapping
    public Result<Map<String, Object>> getStats() {
        return Result.success(statsService.getStats());
    }
}
