package com.qualityvision.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 缺陷明细：一条质检记录对应 N 条缺陷明细，与记录同事务写入。
 * 相比把缺陷塞进 JSON：可 SQL 直接 GROUP BY 做 Pareto/严重度统计，
 * 也方便未来扩展（复核修正某条缺陷、缺陷代码库关联）。
 */
@Data
@TableName("defect_detail")
public class DefectDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private String defectType;
    private String severity;
    private Integer bboxX1;
    private Integer bboxY1;
    private Integer bboxX2;
    private Integer bboxY2;
    private BigDecimal confidence;
    private String description;
    /** 处置建议：放行 / 隔离待评审 / 返工 / 报废 */
    private String suggestion;
    private LocalDateTime createTime;
}
