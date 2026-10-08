package com.qualityvision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qualityvision.domain.InspectionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {

    /** 近 N 天每日 AI 判定合格率（看板趋势图）；since 由调用方按 Java 时间算好，保证 H2/MySQL 双兼容 */
    @Select("SELECT DATE(create_time) AS \"day\", " +
            "COUNT(*) AS \"total\", " +
            "SUM(CASE WHEN overall_result = 'PASS' THEN 1 ELSE 0 END) AS \"pass\" " +
            "FROM inspection_record " +
            "WHERE create_time >= #{since} " +
            "GROUP BY DATE(create_time) ORDER BY \"day\"")
    List<Map<String, Object>> dailyPassRate(@Param("since") java.time.LocalDateTime since);

    /** 各产品合格率对比；since 由调用方按 Java 时间算好，保证 H2/MySQL 双兼容 */
    @Select("SELECT product_id AS \"product_id\", COUNT(*) AS \"total\", " +
            "SUM(CASE WHEN overall_result = 'PASS' THEN 1 ELSE 0 END) AS \"pass\" " +
            "FROM inspection_record " +
            "WHERE create_time >= #{since} " +
            "GROUP BY product_id")
    List<Map<String, Object>> passRateByProduct(@Param("since") java.time.LocalDateTime since);
}
