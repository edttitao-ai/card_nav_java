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

    /**
     * AI 场景用：按侧边栏查询，硬上限 limit 条，pinned 优先 + id 倒序
     */
    List<CardsDo> selectBySidebarIdLimited(@Param("sidebarId") String sidebarId, @Param("limit") Integer limit);

    /**
     * AI 场景用：全量查询，硬上限 limit 条，pinned 优先 + id 倒序
     */
    List<CardsDo> selectAllLimited(@Param("limit") Integer limit);

    /**
     * AI 场景用：按关键词 + 侧边栏 + 分类检索（title/description/url 模糊匹配），pinned 优先 + id 倒序，硬上限 limit
     */
    List<CardsDo> searchCards(@Param("keyword") String keyword,
                              @Param("sidebarId") String sidebarId,
                              @Param("categoryId") Long categoryId,
                              @Param("limit") Integer limit);

    List<CategoryStats> selectCategoryStats();

    List<SidebarStats> selectSidebarStats();

    /**
     * 按 sidebarId 软删除所有卡片
     */
    int softDeleteBySidebarId(String sidebarId);
}
