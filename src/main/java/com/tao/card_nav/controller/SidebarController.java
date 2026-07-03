package com.tao.card_nav.controller;

import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.exception.ErrorCode;
import com.tao.card_nav.exception.ThrowUtils;
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
        ThrowUtils.throwIf(sidebar == null, ErrorCode.PARAMS_ERROR, "侧边栏对象不能为空");
        ThrowUtils.throwIf(sidebar.getId() == null || sidebar.getId().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "侧边栏Id不能为空");
        ThrowUtils.throwIf(sidebar.getLabel() == null || sidebar.getLabel().trim().isEmpty(), ErrorCode.PARAMS_ERROR, "侧边栏名称不能为空");
        sidebarService.addSidebar(sidebar);
        return Result.success();
    }

    /**
     * 更新侧边栏
     */
    @PutMapping("/{id}")
    public Result<Void> updateSidebar(@PathVariable String id, @RequestBody SidebarDo sidebar) {
        ThrowUtils.throwIf(id == null || id.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "侧边栏ID不能为空");
        ThrowUtils.throwIf(sidebar == null, ErrorCode.PARAMS_ERROR, "侧边栏对象不能为空");
        sidebar.setId(id);
        sidebarService.updateSidebar(sidebar);
        return Result.success();
    }

    /**
     * 删除侧边栏
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSidebar(@PathVariable String id) {
        ThrowUtils.throwIf(id == null || id.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "侧边栏ID不能为空");
        sidebarService.deleteSidebar(id);
        return Result.success();
    }
}
