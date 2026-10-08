package com.qualityvision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qualityvision.domain.InspectionRecord;
import com.qualityvision.domain.Product;
import com.qualityvision.dto.ApiResponse;
import com.qualityvision.mapper.ProductMapper;
import com.qualityvision.service.InspectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InspectController {

    private final InspectionService inspectionService;
    private final ProductMapper productMapper;

    @Autowired
    public InspectController(InspectionService inspectionService, ProductMapper productMapper) {
        this.inspectionService = inspectionService;
        this.productMapper = productMapper;
    }

    /** 产品下拉 */
    @GetMapping("/products")
    public ApiResponse<List<Product>> products() {
        return ApiResponse.ok(productMapper.selectList(
                new LambdaQueryWrapper<Product>().orderByAsc(Product::getId)));
    }

    /** 上传并 AI 检测 */
    @PostMapping("/inspect")
    public ApiResponse<InspectionRecord> inspect(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("productId") Long productId,
                                                 @RequestParam(value = "batchNo", required = false) String batchNo) {
        try {
            return ApiResponse.ok(inspectionService.inspect(file, productId, batchNo));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail("检测失败：" + e.getMessage());
        }
    }

    /** 质检记录分页 */
    @GetMapping("/records")
    public ApiResponse<?> records(@RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                  @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                  @RequestParam(value = "productId", required = false) Long productId,
                                  @RequestParam(value = "batchNo", required = false) String batchNo,
                                  @RequestParam(value = "overallResult", required = false) String overallResult,
                                  @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.ok(inspectionService.page(pageNum, pageSize, productId, batchNo, overallResult, status));
    }

    /** 记录详情（含 AI 结果 JSON，前端画框用） */
    @GetMapping("/records/{id}")
    public ApiResponse<InspectionRecord> detail(@PathVariable("id") Long id) {
        InspectionRecord r = inspectionService.get(id);
        return r == null ? ApiResponse.fail("记录不存在") : ApiResponse.ok(r);
    }

    /** 人工复核确认 */
    @PostMapping("/records/{id}/confirm")
    public ApiResponse<InspectionRecord> confirm(@PathVariable("id") Long id,
                                                 @RequestBody Map<String, String> body,
                                                 HttpSession session) {
        try {
            String inspector = String.valueOf(session.getAttribute("username"));
            InspectionRecord r = inspectionService.confirm(id, body.get("verdict"), body.get("note"), inspector);
            return ApiResponse.ok(r);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
