package com.tao.card_nav.service;

import com.tao.card_nav.entity.FavoritesDo;
import com.tao.card_nav.mapper.FavoritesDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritesService {

    private final FavoritesDoMapper favoritesMapper;

    public List<FavoritesDo> getAll() {
        return favoritesMapper.selectAll();
    }

    public void addFavorite(FavoritesDo favorite) {
        FavoritesDo existing = favoritesMapper.selectByCardId(favorite.getCardId());
        if (existing == null) {
            favorite.setCreatedAt(new Date());
            favoritesMapper.insert(favorite);
        }
    }

    public void removeFavorite(Long cardId) {
        favoritesMapper.deleteByCardId(cardId);
    }
}