package com.tao.card_nav.controller;

import com.tao.card_nav.entity.CardDeleteHistoryDo;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.CardDeleteHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 删除历史 admin 查询接口。
 *
 * <p>当前仅"读"——无修改/删除入口（避免误删审计痕迹）。
 * 如需"驳回 / 重新申请"等功能，留待阶段 2。
 */
@RestController
@RequestMapping("/api/admin/delete-history")
@RequiredArgsConstructor
public class DeleteHistoryAdminController {

    private final CardDeleteHistoryService historyService;

    /**
     * 查询最近的删除申请记录。
     *
     * @param limit 上限（默认 50）
     */
    @GetMapping
    public Result<List<CardDeleteHistoryDo>> list(@RequestParam(defaultValue = "50") int limit) {
        return Result.success(historyService.listRecent(limit));
    }
}