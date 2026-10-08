package com.qualityvision.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/** 单个缺陷：类型 + 严重度 + 归一化坐标框（0-1000）+ 置信度 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Defect {
    /** scratch/crack/stain/deformation/missing_part/hole/earloop_defect/seal_defect/unknown */
    private String type;
    /** high/medium/low */
    private String severity;
    /** [x_min, y_min, x_max, y_max]，0-1000 归一化坐标 */
    private List<Integer> bbox;
    private Double confidence;
    private String description;
    /** 处置建议：放行 / 隔离待评审 / 返工 / 报废 */
    private String suggestion;
}
