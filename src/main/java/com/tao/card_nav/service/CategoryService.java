package com.tao.card_nav.service;

import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.mapper.CategoryDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryDoMapper categoryMapper;

    /**
     * 查询所有分类，按 sortOrder 排序
     */
    public List<CategoryDo> getAllCategories() {
        return categoryMapper.selectAllOrderBySortOrder();
    }
}
