package com.qualityvision.service;

import com.qualityvision.dto.Defect;
import com.qualityvision.dto.DefectReport;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Mock 实现：无 key / 离线演示时走这里。
 * 按图片字节哈希确定性地生成" plausible "的检测结果：
 * 同一张图每次结果一致，演示可复现；结果打标 source=MOCK，入库可区分。
 */
@Service
public class MockVisionInspectService implements VisionInspectService {

    @Override
    public String source() {
        return "MOCK";
    }

    @Override
    public DefectReport inspect(byte[] imageBytes, String productName) {
        long start = System.currentTimeMillis();
        int h = Arrays.hashCode(imageBytes);
        int bucket = Math.floorMod(h, 10);

        DefectReport report = new DefectReport();
        report.setSource("MOCK");
        List<Defect> defects = new ArrayList<>();

        if (bucket < 5) {
            // 50%：合格品
            report.setHasDefect(false);
        } else if (bucket < 7) {
            defects.add(defect("stain", "medium", 120, 80, 300, 260, 0.88, "表面有一处约1cm污点", "返工"));
            report.setHasDefect(true);
        } else if (bucket < 8) {
            defects.add(defect("scratch", "medium", 200, 150, 520, 220, 0.85, "表面有一道约1.5cm划痕", "返工"));
            report.setHasDefect(true);
        } else if (bucket < 9) {
            defects.add(defect("deformation", "high", 400, 200, 700, 500, 0.93, "产品本体明显变形", "隔离待评审"));
            defects.add(defect("stain", "low", 150, 120, 230, 200, 0.72, "边缘轻微污点", "放行"));
            report.setHasDefect(true);
        } else {
            defects.add(defect("crack", "high", 250, 180, 600, 240, 0.94, "表面有一道裂纹", "隔离待评审"));
            report.setHasDefect(true);
        }
        report.setDefects(defects);
        report.setLatencyMs(System.currentTimeMillis() - start);
        return report;
    }

    private Defect defect(String type, String severity, int x1, int y1, int x2, int y2,
                          double confidence, String description, String suggestion) {
        Defect d = new Defect();
        d.setType(type);
        d.setSeverity(severity);
        d.setBbox(Arrays.asList(x1, y1, x2, y2));
        d.setConfidence(confidence);
        d.setDescription(description);
        d.setSuggestion(suggestion);
        return d;
    }
}
