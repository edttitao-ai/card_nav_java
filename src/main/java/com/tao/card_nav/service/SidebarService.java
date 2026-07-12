package com.tao.card_nav.service;

import com.tao.card_nav.config.CacheConfig;
import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.exception.BusinessException;
import com.tao.card_nav.mapper.CardsDoMapper;
import com.tao.card_nav.mapper.SidebarDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SidebarService {

    private final SidebarDoMapper sidebarMapper;
    private final CardsDoMapper cardsMapper;

    /**
     * 查询所有侧边栏，按 sortOrder 排序（命中 Caffeine 缓存，TTL 10 分钟）
     */
    @Cacheable(CacheConfig.CACHE_SIDEBARS)
    public List<SidebarDo> getAllSidebars() {
        return sidebarMapper.selectAllOrderBySortOrder();
    }

    /**
     * 按侧边栏 label 精确匹配 ID。
     *
     * <p><b>语义：仅精确 {@code equals} 匹配，不做 {@code contains} 模糊兜底</b>。
     * 消除 "AI 写作" 误命中 "AI 写作工具" 这类静默错误。
     *
     * <p>性能：复用 {@link #getAllSidebars()} 的 Caffeine 缓存，单循环 O(n)；
     * 后续可下沉到 mapper 单条 SQL。
     *
     * @return 命中返回侧边栏 id；未命中返回 {@code null}
     */
    public String resolveIdByLabel(String label) {
        if (label == null) {
            return null;
        }
        for (SidebarDo s : getAllSidebars()) {
            if (s != null && label.equals(s.getLabel())) {
                return s.getId();
            }
        }
        return null;
    }

    /**
     * 新增侧边栏（写后清空缓存）
     */
    @CacheEvict(value = CacheConfig.CACHE_SIDEBARS, allEntries = true)
    public void addSidebar(SidebarDo sidebar) {
        // 检查 ID 是否已存在
        SidebarDo existing = sidebarMapper.selectByPrimaryKey(sidebar.getId());
        if (existing != null) {
            throw new BusinessException(400, "栏目 ID 已存在，请使用其他 ID");
        }
        // 获取当前最大 sortOrder
        List<SidebarDo> all = sidebarMapper.selectAllOrderBySortOrder();
        int maxOrder = all.stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max().orElse(0);
        sidebar.setSortOrder(maxOrder + 1);
        sidebarMapper.insertSelective(sidebar);
    }

    /**
     * 更新侧边栏（写后清空缓存）
     */
    @CacheEvict(value = CacheConfig.CACHE_SIDEBARS, allEntries = true)
    public void updateSidebar(SidebarDo sidebar) {
        sidebarMapper.updateByPrimaryKeySelective(sidebar);
    }

    /**
     * 删除侧边栏（同时删除其下所有卡片；写后清空缓存）
     */
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SIDEBARS, allEntries = true)
    public void deleteSidebar(String id) {
        cardsMapper.softDeleteBySidebarId(id);
        sidebarMapper.deleteByPrimaryKey(id);
    }
}
