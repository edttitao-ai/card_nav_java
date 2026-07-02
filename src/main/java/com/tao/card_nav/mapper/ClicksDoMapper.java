package com.tao.card_nav.mapper;

import com.tao.card_nav.domain.ClickWithCard;
import com.tao.card_nav.entity.ClicksDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ClicksDoMapper {
    int insert(ClicksDo record);

    int insertSelective(ClicksDo record);

    ClicksDo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ClicksDo record);

    int updateByPrimaryKey(ClicksDo record);

    /**
     * 按 card_id 查询点击记录
     */
    ClicksDo selectByCardId(String cardId);

    /**
     * 按 cardId 增加点击次数（原子操作）
     */
    int incrementCount(String cardId);

    /**
     * 查询所有点击记录
     */
    List<ClicksDo> selectAll();

    /**
     * 联表查询点击记录及卡片标题
     */
    List<ClickWithCard> selectAllWithCard();
}
