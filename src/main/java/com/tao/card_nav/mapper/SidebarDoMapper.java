package com.tao.card_nav.mapper;

import com.tao.card_nav.entity.SidebarDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SidebarDoMapper {
    int insert(SidebarDo record);

    int insertSelective(SidebarDo record);

    SidebarDo selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(SidebarDo record);

    int updateByPrimaryKey(SidebarDo record);

    /**
     * 查询所有侧边栏，按 sort_order 排序
     */
    List<SidebarDo> selectAllOrderBySortOrder();

    int deleteByPrimaryKey(String id);
}
