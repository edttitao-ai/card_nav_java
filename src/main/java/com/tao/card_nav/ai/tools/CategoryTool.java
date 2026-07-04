package com.tao.card_nav.ai.tools;

import cn.hutool.json.JSONUtil;
import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.service.CategoryService;
import com.tao.card_nav.service.SidebarService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryTool {

    private final CategoryService categoryService;

    private final SidebarService sidebarService;

    /**
     * 查询所有分类
     * 返回格式: [{"id": 1, "name": "工具", "sortOrder": 1}, ...]
     */
    @Tool(name = "查询分类列表", value = "查询所有卡片分类列表，返回分类ID、名称等信息，用于新增卡片时获取分类ID")
    public String getAllCategories() {
        List<CategoryDo> categories = categoryService.getAllCategories();
        return JSONUtil.toJsonStr(categories);
    }

    /**
     * 查询所有侧边栏（栏目）
     * 返回格式: [{"id": "tools", "label": "工具", "icon": "...", "sortOrder": 1}, ...]
     */
    @Tool(name = "查询侧边栏列表", value = "查询所有侧边栏栏目列表，返回栏目ID、名称、图标等信息，用于新增卡片时获取侧边栏ID")
    public String getAllSidebars() {
        List<SidebarDo> sidebars = sidebarService.getAllSidebars();
        return JSONUtil.toJsonStr(sidebars);
    }
}
