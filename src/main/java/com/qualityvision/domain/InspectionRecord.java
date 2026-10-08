package com.qualityvision.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspection_record")
public class InspectionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String batchNo;
    private String imagePath;
    /** VLM 原始返回，存档备查 */
    private String aiRawResponse;
    /** 清洗校验后的结构化结果 JSON */
    private String aiResultJson;
    private Integer defectCount;
    /** AI 判定：PASS / FAIL */
    private String overallResult;
    /** 结果来源：VLM / MOCK */
    private String source;
    private Long latencyMs;
    /** AI_DONE 待复核 / CONFIRMED 已复核 */
    private String status;
    /** 人工复核结论：PASS / FAIL */
    private String humanVerdict;
    private String humanNote;
    private String inspector;
    private LocalDateTime createTime;
    private LocalDateTime confirmedAt;
}
