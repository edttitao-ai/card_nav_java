package com.tao.card_nav.controller;

import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.SidebarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sidebars")
@RequiredArgsConstructor
public class SidebarController {

    private final SidebarService sidebarService;

    /**
     * 查询所有侧边栏
     */
    @GetMapping
    public Result<List<SidebarDo>> getAllSidebars() {
        return Result.success(sidebarService.getAllSidebars());
    }

    /**
     * 新增侧边栏
     */
    @PostMapping
    public Result<Void> addSidebar(@RequestBody SidebarDo sidebar) {
        sidebarService.addSidebar(sidebar);
        return Result.success();
    }

    /**
     * 更新侧边栏
     */
    @PutMapping("/{id}")
    public Result<Void> updateSidebar(@PathVariable String id, @RequestBody SidebarDo sidebar) {
        sidebar.setId(id);
        sidebarService.updateSidebar(sidebar);
        return Result.success();
    }

    /**
     * 删除侧边栏
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSidebar(@PathVariable String id) {
        sidebarService.deleteSidebar(id);
        return Result.success();
    }
}
