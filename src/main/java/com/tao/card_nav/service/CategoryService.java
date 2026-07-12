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

    /**
     * 按分类名称精确匹配 ID。
     *
     * <p><b>语义：仅精确 {@code equals} 匹配，不做 {@code contains} 模糊兜底</b>。
     * 旧的 "AI 匹配到 AI 写作工具" 这类静默错误由此消除。调用方应负责：
     * <ul>
     *   <li>先把 {@code name} 用 {@code trim()} 归一（避免前后空格导致查不到）；</li>
     *   <li>name 为 null 或空白时直接返回 null，不再额外校验。</li>
     * </ul>
     *
     * <p>性能：复用 {@link #getAllCategories()} 的 Caffeine 缓存，单循环 O(n)；
     * 后续若分类量大，可下沉到 {@code categoryMapper.selectByName(name)} 单条 SQL（O(log n)）。
     *
     * @return 命中返回分类 id；未命中返回 {@code null}
     */
    public Long resolveIdByName(String name) {
        if (name == null) {
            return null;
        }
        for (CategoryDo c : getAllCategories()) {
            if (c != null && name.equals(c.getName())) {
                return c.getId();
            }
        }
        return null;
    }
}
