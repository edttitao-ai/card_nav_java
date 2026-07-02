package com.tao.card_nav.mapper;

import com.tao.card_nav.entity.CategoryDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryDoMapper {
    int insert(CategoryDo record);

    int insertSelective(CategoryDo record);

    CategoryDo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CategoryDo record);

    int updateByPrimaryKey(CategoryDo record);

    /**
     * 查询所有分类，按 sort_order 排序
     */
    List<CategoryDo> selectAllOrderBySortOrder();
}
