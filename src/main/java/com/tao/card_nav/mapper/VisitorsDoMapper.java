package com.tao.card_nav.mapper;

import com.tao.card_nav.entity.VisitorsDo;
import com.tao.card_nav.domain.DayCount;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface VisitorsDoMapper {
    int insert(VisitorsDo record);

    int insertSelective(VisitorsDo record);

    VisitorsDo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(VisitorsDo record);

    int updateByPrimaryKey(VisitorsDo record);

    List<VisitorsDo> selectAll();

    long countAll();

    List<DayCount> selectLastNDaysStats(@Param("days") int days);
}
