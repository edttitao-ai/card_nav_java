package com.tao.card_nav.service;

import com.tao.card_nav.config.CacheConfig;
import com.tao.card_nav.entity.CategoryDo;
import com.tao.card_nav.mapper.CategoryDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryDoMapper categoryMapper;

    /**
     * 查询所有分类，按 sortOrder 排序（命中 Caffeine 缓存，TTL 10 分钟）
     * 当前 CategoryService 没有写方法，靠 TTL 兜底失效；
     * 后续若新增增删改方法，请补 @CacheEvict(CacheConfig.CACHE_CATEGORIES, allEntries = true)
     */
    @Cacheable(CacheConfig.CACHE_CATEGORIES)
    public List<CategoryDo> getAllCategories() {
        return categoryMapper.selectAllOrderBySortOrder();
    }
}
