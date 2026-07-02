package com.tao.card_nav.service;

import com.tao.card_nav.entity.SidebarDo;
import com.tao.card_nav.mapper.CardsDoMapper;
import com.tao.card_nav.mapper.SidebarDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SidebarService {

    private final SidebarDoMapper sidebarMapper;
    private final CardsDoMapper cardsMapper;

    /**
     * 查询所有侧边栏，按 sortOrder 排序
     */
    public List<SidebarDo> getAllSidebars() {
        return sidebarMapper.selectAllOrderBySortOrder();
    }

    /**
     * 新增侧边栏
     */
    public void addSidebar(SidebarDo sidebar) {
        // 获取当前最大 sortOrder
        List<SidebarDo> all = sidebarMapper.selectAllOrderBySortOrder();
        int maxOrder = all.stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max().orElse(0);
        sidebar.setSortOrder(maxOrder + 1);
        sidebarMapper.insertSelective(sidebar);
    }

    /**
     * 更新侧边栏
     */
    public void updateSidebar(SidebarDo sidebar) {
        sidebarMapper.updateByPrimaryKeySelective(sidebar);
    }

    /**
     * 删除侧边栏（同时删除其下所有卡片）
     */
    @Transactional
    public void deleteSidebar(String id) {
        cardsMapper.softDeleteBySidebarId(id);
        sidebarMapper.deleteByPrimaryKey(id);
    }
}
