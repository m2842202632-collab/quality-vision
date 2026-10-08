package com.qualityvision.controller;

import com.qualityvision.dto.ApiResponse;
import com.qualityvision.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    @Autowired
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 看板聚合数据 */
    @GetMapping("/dashboard")
    public ApiResponse<?> dashboard(@RequestParam(value = "days", defaultValue = "14") int days) {
        return ApiResponse.ok(statsService.dashboard(Math.min(days, 90)));
    }
}
