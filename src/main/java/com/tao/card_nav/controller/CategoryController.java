package com.tao.card_nav.controller;

import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.result.Result;
import com.tao.card_nav.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 查询所有分类
     */
    @GetMapping
    public Result<List<CategoryDo>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }
}
