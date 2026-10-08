package com.qualityvision.service;

import com.qualityvision.config.VlmProperties;
import com.qualityvision.dto.DefectReport;
import com.qualityvision.util.JsonCleaner;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;

/**
* 真实 VLM 实现：LangChain4j OpenAI 兼容模块。
* 默认走阿里百炼 qwen-vl-max；换厂商只改配置（base-url/api-key/model），代码不动。
* 防线：低温(0.2) + 封闭枚举prompt + JsonCleaner四层清洗 + 失败一次repair-loop + 最终降级Mock。
*/
@Slf4j
@Service
public class LlmVisionInspectService implements VisionInspectService {

private static final String SYSTEM_PROMPT = """
你是医疗器械产线的视觉质检员，只检查产品表面缺陷。
缺陷类型只允许：scratch（划痕：细长条状亮线）、crack（裂纹：不规则开裂缝隙）、
stain（污点：颜色异常斑块）、deformation（变形：形状扭曲褶皱）、
missing_part（缺件：应有部件缺失）、hole（破洞：贯穿性孔洞）。
严重度只允许：high（影响功能/安全）、medium（外观明显）、low（轻微/不确定）。
良品标准：表面光洁无划痕污渍、形状规整无变形、部件齐全、包装完好。
处置建议规则：high→隔离待评审；medium→返工；low→放行（记录跟踪）。
""";

private static final String USER_PROMPT_TEMPLATE = """
分析这张【%s】的质检照片。
只输出一个严格合法的 JSON 对象，不要输出任何其他文字，不要用 markdown 代码块包裹，不要解释。
JSON 格式：
{"has_defect": true, "defects": [{"type": "scratch", "severity": "high",
"bbox": [120, 45, 380, 520], "confidence": 0.92, "description": "左上边缘有一道约2cm划痕",
"suggestion": "隔离待评审"}]}
规则：
1. bbox 是归一化 0-1000 相对坐标 [x_min, y_min, x_max, y_max]，左上为原点；多个缺陷按 confidence 从高到低排序。
2. 如果没有发现缺陷，必须返回 {"has_defect": false, "defects": []}，绝不编造缺陷。
3. 如果图片内容明显不是待检产品照片（纯文字文档、屏幕截图、风景、人物、无关物体等），必须返回 {"has_defect": true, "defects": [{"type": "missing_part", "severity": "high", "bbox": [0, 0, 1000, 1000], "confidence": 0.99, "description": "图片不是有效的产品质检照片，无法判定合格", "suggestion": "隔离待评审"}]}。
""";

private final VlmProperties props;
private final MockVisionInspectService fallback;
private volatile ChatModel cachedModel;

@Autowired
public LlmVisionInspectService(VlmProperties props, MockVisionInspectService fallback) {
this.props = props;
this.fallback = fallback;
}

@Override
public String source() {
return "VLM";
}

@Override
public DefectReport inspect(byte[] imageBytes, String productName) {
long start = System.currentTimeMillis();
String base64 = Base64.getEncoder().encodeToString(imageBytes);
String prompt = String.format(USER_PROMPT_TEMPLATE, productName);
try {
String raw = chat(prompt, base64);
DefectReport report = safeParse(raw, prompt, base64);
report.setSource("VLM");
report.setRawResponse(raw);
report.setLatencyMs(System.currentTimeMillis() - start);
return report;
} catch (Exception e) {
// 最终防线：降级为 Mock，保证接口永不抛错，结果打标可区分
log.warn("VLM 质检失败，已降级为 Mock: {} - {}", e.getClass().getSimpleName(), e.getMessage());
DefectReport mock = fallback.inspect(imageBytes, productName);
mock.setLatencyMs(System.currentTimeMillis() - start);
return mock;
}
}

private String chat(String prompt, String base64) {
ChatModel model = model();
UserMessage msg = UserMessage.from(
TextContent.from(SYSTEM_PROMPT + "\n" + prompt),
ImageContent.from(base64, "image/jpeg"));
ChatResponse response = model.chat(msg);
return response.aiMessage().text();
}

/** 解析 + 失败时喂回错误做一次 repair-loop */
private DefectReport safeParse(String raw, String prompt, String base64) {
try {
return JsonCleaner.parse(raw);
} catch (Exception first) {
log.info("VLM 输出解析失败，尝试 repair-loop: {}", first.getMessage());
String repairPrompt = "上一次输出不是合法 JSON，解析错误：" + first.getMessage()
+ "。请只输出 JSON 对象，不要任何其他文字，不要用 markdown 包裹。\n" + prompt;
try {
String retry = chat(repairPrompt, base64);
return JsonCleaner.parse(retry);
} catch (Exception second) {
throw new IllegalStateException("VLM 输出两次解析均失败: " + second.getMessage(), second);
}
}
}

private ChatModel model() {
if (cachedModel == null) {
synchronized (this) {
if (cachedModel == null) {
var b = OpenAiChatModel.builder()
.baseUrl(props.getBaseUrl())
.apiKey(props.getApiKey())
.modelName(props.getModel())
.temperature(props.getTemperature())
.timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
.maxRetries(props.getMaxRetries());
cachedModel = b.build();
}
}
}
return cachedModel;
}
}
