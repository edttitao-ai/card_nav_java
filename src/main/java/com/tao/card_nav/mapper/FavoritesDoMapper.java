package com.tao.card_nav.mapper;

import com.tao.card_nav.entity.FavoritesDo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FavoritesDoMapper {
    int insert(FavoritesDo record);

    int deleteByCardId(long cardId);

    List<FavoritesDo> selectAll();

    FavoritesDo selectByCardId(Long cardId);
}