package com.qualityvision;

import com.qualityvision.dto.DefectReport;
import com.qualityvision.util.JsonCleaner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonCleaner 四层防线单测：markdown 包裹 / 非法枚举 / 坐标越界 / 缺失字段
 */
class JsonCleanerTest {

    @Test
    void markdownWrappedJson() throws Exception {
        String raw = "好的，这是检测结果：\n```json\n{\"has_defect\": true, \"defects\": [" +
                "{\"type\": \"scratch\", \"severity\": \"high\", \"bbox\": [10, 20, 300, 400], " +
                "\"confidence\": 0.9, \"description\": \"划痕\"}]}\n```";
        DefectReport r = JsonCleaner.parse(raw);
        assertTrue(r.isHasDefect());
        assertEquals(1, r.getDefects().size());
        assertEquals("scratch", r.getDefects().get(0).getType());
    }

    @Test
    void illegalEnumFixed() throws Exception {
        String raw = "{\"has_defect\": true, \"defects\": [" +
                "{\"type\": \"疑似划痕\", \"severity\": \"非常严重\", \"bbox\": [0, 0, 100, 100]}]}";
        DefectReport r = JsonCleaner.parse(raw);
        assertEquals("unknown", r.getDefects().get(0).getType());
        assertEquals("medium", r.getDefects().get(0).getSeverity());
        assertEquals(0.5, r.getDefects().get(0).getConfidence());
    }

    @Test
    void bboxClamped() throws Exception {
        String raw = "{\"has_defect\": true, \"defects\": [" +
                "{\"type\": \"stain\", \"severity\": \"low\", \"bbox\": [-50, 0, 5000, 800]}]}";
        DefectReport r = JsonCleaner.parse(raw);
        assertEquals(java.util.List.of(0, 0, 1000, 800), r.getDefects().get(0).getBbox());
    }

    @Test
    void emptyDefectsMeansPass() throws Exception {
        DefectReport r = JsonCleaner.parse("{\"has_defect\": false, \"defects\": []}");
        assertFalse(r.isHasDefect());
        assertTrue(r.getDefects().isEmpty());
    }

    @Test
    void garbageThrows() {
        assertThrows(Exception.class, () -> JsonCleaner.parse("这不是JSON"));
    }
}
