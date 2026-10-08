package com.qualityvision.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualityvision.dto.Defect;
import com.qualityvision.dto.DefectReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VLM 输出清洗与校验：四层防线。
 * 1. 预清洗：去 markdown 包裹 / 前导废话，提取第一个完整 JSON
 * 2. Jackson 宽容解析（忽略未知字段）
 * 3. 字段校验与修复（枚举收敛、坐标钳制、缺失默认值）
 * 4. 失败抛异常由上层 repair-loop 或降级处理
 */
public class JsonCleaner {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Pattern JSON_BLOCK = Pattern.compile("(?s)\\{.*\\}");

    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "scratch", "crack", "stain", "deformation", "missing_part",
            "hole", "earloop_defect", "seal_defect"));

    private static final Set<String> ALLOWED_SEVERITY = new HashSet<>(
            Arrays.asList("high", "medium", "low"));

    public static DefectReport parse(String raw) throws Exception {
        String jsonText = extractJson(raw);
        DefectReport report = MAPPER.readValue(jsonText, DefectReport.class);
        if (report.getDefects() == null) {
            report.setDefects(new ArrayList<>());
        }
        for (Defect d : report.getDefects()) {
            fixDefect(d);
        }
        // has_defect 与 defects 自洽：有缺陷条目则视为有缺陷
        if (!report.getDefects().isEmpty()) {
            report.setHasDefect(true);
        }
        return report;
    }

    /** 第 1 层：提取第一个 {...} 块 */
    static String extractJson(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("VLM 返回为空");
        }
        String s = raw.replace("```json", "").replace("```", "").trim();
        Matcher m = JSON_BLOCK.matcher(s);
        if (m.find()) {
            return m.group();
        }
        return s;
    }

    /** 第 3 层：字段校验与修复 */
    static void fixDefect(Defect d) {
        if (d.getType() == null || !ALLOWED_TYPES.contains(d.getType().toLowerCase())) {
            d.setType("unknown");
        } else {
            d.setType(d.getType().toLowerCase());
        }
        if (d.getSeverity() == null || !ALLOWED_SEVERITY.contains(d.getSeverity().toLowerCase())) {
            d.setSeverity("medium");
        } else {
            d.setSeverity(d.getSeverity().toLowerCase());
        }
        d.setBbox(clampBbox(d.getBbox()));
        if (d.getConfidence() == null || d.getConfidence() < 0 || d.getConfidence() > 1) {
            d.setConfidence(0.5);
        }
        if (d.getDescription() == null) {
            d.setDescription("");
        }
        if (d.getSuggestion() == null) {
            // 处置建议默认按严重度映射：high→隔离待评审，medium→返工，low→放行
            switch (d.getSeverity()) {
                case "high": d.setSuggestion("隔离待评审"); break;
                case "low": d.setSuggestion("放行"); break;
                default: d.setSuggestion("返工");
            }
        }
    }

    private static List<Integer> clampBbox(List<Integer> bbox) {
        List<Integer> out = new ArrayList<>(Arrays.asList(0, 0, 0, 0));
        if (bbox != null) {
            for (int i = 0; i < Math.min(4, bbox.size()); i++) {
                Integer v = bbox.get(i);
                out.set(i, v == null ? 0 : Math.max(0, Math.min(1000, v)));
            }
        }
        // 保证 x_min<=x_max, y_min<=y_max，否则给一个默认小框
        if (out.get(0) >= out.get(2) || out.get(1) >= out.get(3)) {
            return new ArrayList<>(Arrays.asList(400, 400, 600, 600));
        }
        return out;
    }
}
