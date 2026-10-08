package com.qualityvision.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VLM 配置：厂商切换只需改 base-url / api-key / model 三处，业务代码零改动。
 * mock-enabled=true 时不调任何外部接口，直接走本地 Mock，离线可演示。
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlm")
public class VlmProperties {
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String apiKey = "";
    private String model = "qwen-vl-max";
    private double temperature = 0.2;
    private int timeoutSeconds = 60;
    private int maxRetries = 2;
    private boolean mockEnabled = true;
}
