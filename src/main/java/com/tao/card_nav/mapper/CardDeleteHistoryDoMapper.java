package com.tao.card_nav.mapper;

import com.tao.card_nav.entity.CardDeleteHistoryDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * card_delete_history 表的 MyBatis mapper。
 *
 * <p>本表为"纯记录"用途，不参与删除的状态机决策；写入即可。
 * Redis 验证码机制独立于本表（详见 DeleteChallengeService）。
 */
@Mapper
public interface CardDeleteHistoryDoMapper {

    /**
     * 插入一条历史记录；用 useGeneratedKeys 回填 id。
     */
    int insertSelective(CardDeleteHistoryDo record);

    /**
     * 按创建时间倒序查询最近 limit 条。
     *
     * @param limit 上限；null 或非正 → 默认 50
     */
    List<CardDeleteHistoryDo> selectAllOrderByCreatedAtDesc(@Param("limit") Integer limit);
}