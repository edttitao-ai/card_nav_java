package com.tao.card_nav.service;

import com.tao.card_nav.domain.CategoryStats;
import com.tao.card_nav.domain.DayCount;
import com.tao.card_nav.mapper.CardsDoMapper;
import com.tao.card_nav.mapper.VisitorsDoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final VisitorsDoMapper visitorsMapper;
    private final CardsDoMapper cardsMapper;

    private static final int LAST_7_DAYS = 7;

    /**
     * 获取统计数据
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVisits", visitorsMapper.countAll());
        stats.put("last7Days", getLast7DaysStats());
        stats.put("categories", getCategoryStats());
        return stats;
    }

    private List<Map<String, Object>> getLast7DaysStats() {
        List<DayCount> rawData = visitorsMapper.selectLastNDaysStats(LAST_7_DAYS);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Long> dateCountMap = new LinkedHashMap<>();
        for (DayCount dc : rawData) {
            dateCountMap.put(dc.getDate(), dc.getCount());
        }
        Calendar cal = Calendar.getInstance();
        for (int i = LAST_7_DAYS - 1; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = String.format("%02d-%02d", 
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            Map<String, Object> dayStat = new LinkedHashMap<>();
            dayStat.put("date", dateStr);
            dayStat.put("count", dateCountMap.getOrDefault(dateStr, 0L));
            result.add(dayStat);
        }
        return result;
    }

    private List<Map<String, Object>> getCategoryStats() {
        List<CategoryStats> rawData = cardsMapper.selectCategoryStats();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CategoryStats cs : rawData) {
            Map<String, Object> catStat = new LinkedHashMap<>();
            catStat.put("name", cs.getName());
            catStat.put("cardCount", cs.getCardCount());
            catStat.put("clickCount", cs.getClickCount());
            result.add(catStat);
        }
        return result;
    }
}
