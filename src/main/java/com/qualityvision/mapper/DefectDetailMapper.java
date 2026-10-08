package com.qualityvision.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qualityvision.domain.DefectDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DefectDetailMapper extends BaseMapper<DefectDetail> {

    /** 缺陷类型 Pareto：直接 GROUP BY，比解析 JSON 快且准；since 由调用方按 Java 时间算好 */
    @Select("SELECT d.defect_type AS \"type\", COUNT(*) AS \"count\" " +
            "FROM defect_detail d JOIN inspection_record r ON d.record_id = r.id " +
            "WHERE r.create_time >= #{since} " +
            "GROUP BY d.defect_type ORDER BY \"count\" DESC")
    List<Map<String, Object>> paretoByType(@Param("since") java.time.LocalDateTime since);

    /** 严重度分布；since 由调用方按 Java 时间算好 */
    @Select("SELECT d.severity AS \"severity\", COUNT(*) AS \"count\" " +
            "FROM defect_detail d JOIN inspection_record r ON d.record_id = r.id " +
            "WHERE r.create_time >= #{since} " +
            "GROUP BY d.severity")
    List<Map<String, Object>> countBySeverity(@Param("since") java.time.LocalDateTime since);
}
