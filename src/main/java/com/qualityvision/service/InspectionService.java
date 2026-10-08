package com.qualityvision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualityvision.config.VlmProperties;
import com.qualityvision.domain.DefectDetail;
import com.qualityvision.domain.InspectionRecord;
import com.qualityvision.domain.Product;
import com.qualityvision.dto.Defect;
import com.qualityvision.dto.DefectReport;
import com.qualityvision.mapper.DefectDetailMapper;
import com.qualityvision.mapper.InspectionRecordMapper;
import com.qualityvision.mapper.ProductMapper;
import com.qualityvision.util.ImageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 质检核心流程：上传 → AI 检测 → 入库（待复核）→ 人工复核确认。
 * AI 检测是慢调用（3-15s），此处同步实现保证演示简单可靠；
 * 生产级可改为 @Async + 轮询/SSE。
 */
@Service
public class InspectionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InspectionRecordMapper recordMapper;
    private final DefectDetailMapper defectDetailMapper;
    private final ProductMapper productMapper;
    private final VlmProperties vlmProps;
    private final MockVisionInspectService mockService;
    private final LlmVisionInspectService llmService;

    @Value("${qv.upload-dir:./uploads}")
    private String uploadDir;

    @Autowired
    public InspectionService(InspectionRecordMapper recordMapper, DefectDetailMapper defectDetailMapper,
                             ProductMapper productMapper,
                             VlmProperties vlmProps, MockVisionInspectService mockService,
                             LlmVisionInspectService llmService) {
        this.recordMapper = recordMapper;
        this.defectDetailMapper = defectDetailMapper;
        this.productMapper = productMapper;
        this.vlmProps = vlmProps;
        this.mockService = mockService;
        this.llmService = llmService;
    }

    /** 上传并检测：存图 → 调 AI → 落库，返回记录 id */
    @Transactional
    public InspectionRecord inspect(MultipartFile file, Long productId, String batchNo) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传质检照片");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("产品不存在");
        }
        if (!StringUtils.hasText(batchNo)) {
            batchNo = "B" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        }

        // 1. 存原图
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path dir = Paths.get(uploadDir, day);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + ".jpg";
        byte[] raw = file.getBytes();
        Files.write(dir.resolve(filename), raw);
        String imagePath = "uploads/" + day + "/" + filename;

        // 2. 图片预处理：压缩到长边≤1024px 转 JPEG（省 token、降延迟）
        byte[] compressed = ImageUtil.compressToJpeg(raw);

        // 3. 调 AI（开关决定 Mock 还是真实 VLM；VLM 失败自动降级 Mock）
        VisionInspectService vision = vlmProps.isMockEnabled() ? mockService : llmService;
        DefectReport report = vision.inspect(compressed, product.getName());

        // 4. 落库：原始返回存档 + 结构化结果 + 来源打标
        InspectionRecord record = new InspectionRecord();
        record.setProductId(productId);
        record.setBatchNo(batchNo);
        record.setImagePath(imagePath);
        record.setAiRawResponse(report.getRawResponse());
        record.setAiResultJson(MAPPER.writeValueAsString(report));
        record.setDefectCount(report.getDefects() == null ? 0 : report.getDefects().size());
        record.setOverallResult(report.isHasDefect() ? "FAIL" : "PASS");
        record.setSource(report.getSource());
        record.setLatencyMs(report.getLatencyMs());
        record.setStatus("AI_DONE");
        recordMapper.insert(record);

        // 5. 缺陷明细同事务写入 defect_detail（一条记录 N 条明细，SQL 可直接做 Pareto 统计）
        if (report.getDefects() != null) {
            for (Defect d : report.getDefects()) {
                DefectDetail detail = new DefectDetail();
                detail.setRecordId(record.getId());
                detail.setDefectType(d.getType());
                detail.setSeverity(d.getSeverity());
                if (d.getBbox() != null && d.getBbox().size() == 4) {
                    detail.setBboxX1(d.getBbox().get(0));
                    detail.setBboxY1(d.getBbox().get(1));
                    detail.setBboxX2(d.getBbox().get(2));
                    detail.setBboxY2(d.getBbox().get(3));
                }
                if (d.getConfidence() != null) {
                    detail.setConfidence(java.math.BigDecimal.valueOf(d.getConfidence()));
                }
                detail.setDescription(d.getDescription());
                detail.setSuggestion(d.getSuggestion());
                defectDetailMapper.insert(detail);
            }
        }
        return record;
    }

    /** 人工复核：确认 AI 结论或修正。已复核的记录重复确认直接返回（幂等）。 */
    @Transactional
    public InspectionRecord confirm(Long id, String verdict, String note, String inspector) {
        InspectionRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("质检记录不存在");
        }
        if ("CONFIRMED".equals(record.getStatus())) {
            return record;
        }
        if (!"PASS".equals(verdict) && !"FAIL".equals(verdict)) {
            throw new IllegalArgumentException("复核结论只能是 PASS 或 FAIL");
        }
        record.setHumanVerdict(verdict);
        record.setHumanNote(note);
        record.setInspector(inspector);
        record.setStatus("CONFIRMED");
        record.setConfirmedAt(LocalDateTime.now());
        recordMapper.updateById(record);
        return record;
    }

    /** 分页查询：可按产品 / 批次 / AI结论 / 状态筛选 */
    public Page<InspectionRecord> page(int pageNum, int pageSize, Long productId,
                                       String batchNo, String overallResult, String status) {
        LambdaQueryWrapper<InspectionRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(productId != null, InspectionRecord::getProductId, productId);
        qw.like(StringUtils.hasText(batchNo), InspectionRecord::getBatchNo, batchNo);
        qw.eq(StringUtils.hasText(overallResult), InspectionRecord::getOverallResult, overallResult);
        qw.eq(StringUtils.hasText(status), InspectionRecord::getStatus, status);
        qw.orderByDesc(InspectionRecord::getCreateTime);
        return recordMapper.selectPage(new Page<>(pageNum, pageSize), qw);
    }

    public InspectionRecord get(Long id) {
        return recordMapper.selectById(id);
    }
}
