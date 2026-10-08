package com.qualityvision.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** AI 质检结构化结果。metadata 字段不参与 AI JSON 序列化，只用于业务流转。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefectReport {
    private boolean hasDefect;
    private List<Defect> defects = new ArrayList<>();

    @JsonIgnore
    private String source = "MOCK";
    @JsonIgnore
    private long latencyMs;
    @JsonIgnore
    private String rawResponse;

    public static DefectReport pass(String source, long latencyMs) {
        DefectReport r = new DefectReport();
        r.setHasDefect(false);
        r.setSource(source);
        r.setLatencyMs(latencyMs);
        return r;
    }
}
