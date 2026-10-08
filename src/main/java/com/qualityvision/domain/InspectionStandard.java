package com.qualityvision.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_standard")
public class InspectionStandard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String checkItem;
    /** 该检测项关注的缺陷类型 JSON 数组 */
    private String defectTypes;
    /** 严重度判定规则描述 */
    private String severityRules;
    private LocalDateTime createTime;
}
