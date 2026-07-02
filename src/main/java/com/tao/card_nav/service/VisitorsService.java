package com.tao.card_nav.service;

import com.tao.card_nav.entity.VisitorsDo;
import com.tao.card_nav.mapper.VisitorsDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorsService {

    private final VisitorsDoMapper visitorsMapper;

    /**
     * 获取访客日志列表（限制条数）
     */
    public List<VisitorsDo> getVisitors(int limit) {
        List<VisitorsDo> all = visitorsMapper.selectAll();
        if (all.size() > limit) {
            return all.subList(0, limit);
        }
        return all;
    }

    /**
     * 插入访客记录
     */
    public void insert(VisitorsDo visitor) {
        visitorsMapper.insertSelective(visitor);
    }
}