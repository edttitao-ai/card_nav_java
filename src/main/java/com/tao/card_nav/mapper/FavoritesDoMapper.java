package com.tao.card_nav.mapper;

import com.tao.card_nav.domain.FavoriteWithCard;
import com.tao.card_nav.entity.FavoritesDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FavoritesDoMapper {
    int insert(FavoritesDo record);

    int deleteByCardId(long cardId);

    List<FavoritesDo> selectAll();

    FavoritesDo selectByCardId(Long cardId);

    /**
     * 查询所有收藏，联表 cards 实时获取最新的分类和图标信息
     */
    List<FavoriteWithCard> selectAllWithCard();
}