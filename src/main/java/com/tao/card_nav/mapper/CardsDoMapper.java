package com.tao.card_nav.mapper;

import com.tao.card_nav.entity.CardsDo;
import com.tao.card_nav.domain.CategoryStats;
import com.tao.card_nav.domain.SidebarStats;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CardsDoMapper {
    int insert(CardsDo record);

    int insertSelective(CardsDo record);

    CardsDo selectByPrimaryKey(Long id);

    /**
     * 按 URL 查重（排除软删除），用于新增前的去重
     */
    CardsDo selectByUrl(String url);

    /**
     * 按 title 查重（排除软删除），可选的辅助查重
     */
    CardsDo selectByTitle(String title);

    int updateByPrimaryKeySelective(CardsDo record);

    int updateByPrimaryKey(CardsDo record);

    int softDelete(Long id);

    List<CardsDo> selectBySidebarId(String sidebarId);

    List<CardsDo> selectAll();

    List<CategoryStats> selectCategoryStats();

    List<SidebarStats> selectSidebarStats();

    /**
     * 按 sidebarId 软删除所有卡片
     */
    int softDeleteBySidebarId(String sidebarId);
}
