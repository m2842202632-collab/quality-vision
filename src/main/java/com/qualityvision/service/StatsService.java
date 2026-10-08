package com.qualityvision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qualityvision.domain.InspectionRecord;
import com.qualityvision.domain.Product;
import com.qualityvision.mapper.DefectDetailMapper;
import com.qualityvision.mapper.InspectionRecordMapper;
import com.qualityvision.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板统计：合格率趋势、缺陷类型 Pareto、严重度分布、产品合格率对比、待复核数。
 */
@Service
public class StatsService {

    private final InspectionRecordMapper recordMapper;
    private final DefectDetailMapper defectDetailMapper;
    private final ProductMapper productMapper;

    @Autowired
    public StatsService(InspectionRecordMapper recordMapper, DefectDetailMapper defectDetailMapper,
                        ProductMapper productMapper) {
        this.recordMapper = recordMapper;
        this.defectDetailMapper = defectDetailMapper;
        this.productMapper = productMapper;
    }

    public Map<String, Object> dashboard(int days) {
        Map<String, Object> out = new LinkedHashMap<>();

        // 1. 每日合格率趋势 + 总体合格率
        List<String> dates = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        long total = 0, pass = 0;
        for (Map<String, Object> row : recordMapper.dailyPassRate(java.time.LocalDateTime.now().minusDays(days))) {
            dates.add(String.valueOf(row.get("day")));
            long t = ((Number) row.get("total")).longValue();
            long p = ((Number) row.get("pass")).longValue();
            total += t;
            pass += p;
            rates.add(pct(p, t));
        }
        Map<String, Object> trend = new LinkedHashMap<>();
        trend.put("dates", dates);
        trend.put("rates", rates);
        out.put("trend", trend);
        out.put("total", total);
        out.put("passRate", pct(pass, total));
        out.put("pending", pendingCount());

        // 2. 缺陷类型 Pareto + 严重度分布：defect_detail 直接 GROUP BY
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(days);
        out.put("defectPareto", defectDetailMapper.paretoByType(since));
        Map<String, Object> sevDist = new LinkedHashMap<>();
        for (Map<String, Object> row : defectDetailMapper.countBySeverity(since)) {
            sevDist.put(String.valueOf(row.get("severity")), row.get("count"));
        }
        out.put("severityDist", sevDist);

        // 3. 各产品合格率对比
        List<Map<String, Object>> products = new ArrayList<>();
        for (Map<String, Object> row : recordMapper.passRateByProduct(since)) {
            Long pid = ((Number) row.get("product_id")).longValue();
            Product p = productMapper.selectById(pid);
            long t = ((Number) row.get("total")).longValue();
            long pcount = ((Number) row.get("pass")).longValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", p == null ? "未知产品" : p.getName());
            item.put("total", t);
            item.put("passRate", pct(pcount, t));
            products.add(item);
        }
        out.put("byProduct", products);
        return out;
    }

    public long pendingCount() {
        LambdaQueryWrapper<InspectionRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InspectionRecord::getStatus, "AI_DONE");
        return recordMapper.selectCount(qw);
    }

    private static double pct(long part, long total) {
        if (total == 0) {
            return 100.0;
        }
        return Math.round(part * 10000.0 / total) / 100.0;
    }
}
