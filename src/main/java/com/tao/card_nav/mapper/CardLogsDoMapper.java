package com.tao.card_nav.mapper;

import com.tao.card_nav.domain.CardLogWithCard;
import com.tao.card_nav.entity.CardLogsDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CardLogsDoMapper {
    int insert(CardLogsDo record);

    int insertSelective(CardLogsDo record);

    CardLogsDo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CardLogsDo record);

    int updateByPrimaryKey(CardLogsDo record);

    /**
     * 按 card_id 查询操作日志，按时间倒序
     */
    List<CardLogsDo> selectByCardId(Long cardId);

    /**
     * 按 card_id 联表查询日志及卡片标题
     */
    List<CardLogWithCard> selectByCardIdWithTitle(Long cardId);

    /**
     * 全量查询所有操作日志，联表查出卡片标题、栏目名称、分类名称
     */
    List<CardLogWithCard> selectAll();
}
