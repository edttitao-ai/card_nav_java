package com.tao.card_nav.service;

import com.tao.card_nav.domain.CategoryStats;
import com.tao.card_nav.domain.DayCount;
import com.tao.card_nav.domain.SidebarStats;
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
        stats.put("sidebars", getSidebarStats());
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

    private List<Map<String, Object>> getSidebarStats() {
        List<SidebarStats> rawData = cardsMapper.selectSidebarStats();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SidebarStats ss : rawData) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("name", ss.getName());
            stat.put("sidebarId", ss.getSidebarId());
            stat.put("cardCount", ss.getCardCount());
            stat.put("clickCount", ss.getClickCount());
            result.add(stat);
        }
        return result;
    }
}
