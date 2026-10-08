package com.qualityvision.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qualityvision.domain.InspectionStandard;
import com.qualityvision.dto.ApiResponse;
import com.qualityvision.mapper.InspectionStandardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standards")
public class StandardController {

    private final InspectionStandardMapper standardMapper;

    @Autowired
    public StandardController(InspectionStandardMapper standardMapper) {
        this.standardMapper = standardMapper;
    }

    @GetMapping
    public ApiResponse<List<InspectionStandard>> list(
            @RequestParam(value = "productId", required = false) Long productId) {
        LambdaQueryWrapper<InspectionStandard> qw = new LambdaQueryWrapper<>();
        qw.eq(productId != null, InspectionStandard::getProductId, productId)
          .orderByAsc(InspectionStandard::getId);
        return ApiResponse.ok(standardMapper.selectList(qw));
    }

    @PostMapping
    public ApiResponse<InspectionStandard> create(@RequestBody InspectionStandard standard) {
        standardMapper.insert(standard);
        return ApiResponse.ok(standard);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        standardMapper.deleteById(id);
        return ApiResponse.ok(null);
    }
}
