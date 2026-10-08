package com.qualityvision.service;

/**
 * 视觉质检 AI 接口。Mock 与真实 VLM 双实现，
 * 由 vlm.mock-enabled 开关决定走哪个，业务代码无感知。
 */
public interface VisionInspectService {

    com.qualityvision.dto.DefectReport inspect(byte[] imageBytes, String productName);

    /** 实现来源标识：MOCK / VLM */
    String source();
}
