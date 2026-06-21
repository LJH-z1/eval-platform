package com.mavis.evalplatform.evaluation.adapter;

import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.model.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenAI-compatible chat/completions 协议适配器(基类)
 * <p>
 * 支持的提供商:M3、OpenAI、智谱 GLM、通义千问、Moonshot Kimi。
 * <p>
 * 共同点:
 * <ul>
 *   <li>POST {endpoint}/chat/completions 或直接 {endpoint}</li>
 *   <li>Header: Authorization: Bearer {apiKey}, Content-Type: application/json</li>
 *   <li>Body: { model, messages, temperature, max_tokens }</li>
 *   <li>Response: { choices: [{ message: { content } }], usage: { prompt_tokens, completion_tokens } }</li>
 * </ul>
 * <p>
 * 唯一区别:每个 provider 的 endpoint 不同(由子类 {@link #endpoint()} 返回)。
 *
 * @author 梁倩倩 / 向锏楠
 */
@Slf4j
public abstract class OpenAiCompatibleAdapter implements ModelAdapter {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public final ModelCallResult call(ModelConfig model, String question) {
        long start = System.currentTimeMillis();

        // === MOCK 模式 ===
        // 触发条件:apiKey == "MOCK" 或 endpoint 以 "mock://" 开头
        // 用途:不接真 API 也能完整跑通"创建评测→运行→评分"流程
        if (isMockMode(model)) {
            return mockCall(model, question, start);
        }

        try {
            // 1) 校验参数
            if (model.getEndpoint() == null || model.getEndpoint().isBlank()) {
                return ModelCallResult.fail("MISSING_ENDPOINT",
                        "模型未配置 endpoint", System.currentTimeMillis() - start);
            }
            // 2) 构造请求体
            String body = buildRequestBody(model, question);
            // 3) 发送
            URI uri = URI.create(model.getEndpoint());
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer " + model.getApiKey())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            if (resp.statusCode() / 100 != 2) {
                return ModelCallResult.fail("HTTP_" + resp.statusCode(),
                        "HTTP " + resp.statusCode() + ": " + truncate(resp.body(), 300),
                        latency);
            }
            // 4) 解析响应
            return parseResponse(resp.body(), latency);
        } catch (Exception e) {
            log.error("[{}] call failed", provider(), e);
            return ModelCallResult.fail("EXCEPTION",
                    e.getClass().getSimpleName() + ": " + e.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }

    /**
     * 子类需要返回对应的 endpoint(完整 URL)。
     * 如果 model.endpoint 已经有值,优先用 model.endpoint(用户自定义),否则用此默认。
     */
    protected abstract String defaultEndpoint();

    /**
     * 子类可以覆盖默认的 modelVersion 字段(若 model.modelVersion 为空)
     */
    protected String defaultModelVersion() {
        return "";
    }

    private String buildRequestBody(ModelConfig model, String question) {
        String modelName = !isBlank(model.getModelVersion())
                ? model.getModelVersion()
                : defaultModelVersion();
        if (isBlank(modelName)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    provider() + " 模型未配置 modelVersion,且适配器也没有默认值");
        }
        return "{" +
                "\"model\":\"" + escape(modelName) + "\"," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"" + escape(question) + "\"}]," +
                "\"temperature\":" + (model.getTemperature() != null ? model.getTemperature().toPlainString() : "0.7") + "," +
                "\"max_tokens\":" + (model.getMaxTokens() != null ? model.getMaxTokens() : 1024) +
                "}";
    }

    /**
     * 解析 OpenAI 兼容响应,提取 content / token 信息。
     * 子类可重写以适配不同响应结构。
     */
    protected ModelCallResult parseResponse(String body, long latency) {
        String content = extractJsonField(body, "\"content\":");
        Integer promptTokens = extractJsonInt(body, "\"prompt_tokens\":");
        Integer completionTokens = extractJsonInt(body, "\"completion_tokens\":");
        if (content == null) {
            // 模型可能因为多种原因没返回 content(限流、超时)
            return ModelCallResult.fail("EMPTY_RESPONSE",
                    "响应中未找到 content 字段:" + truncate(body, 200), latency);
        }
        return ModelCallResult.ok(content,
                promptTokens != null ? promptTokens : 0,
                completionTokens != null ? completionTokens : 0,
                latency);
    }

    // ---- 工具方法 ----

    /**
     * 判断是否走 mock 模式
     */
    private boolean isMockMode(ModelConfig model) {
        if (model == null) return false;
        String key = model.getApiKey();
        if ("MOCK".equalsIgnoreCase(key) || "sk-MOCK".equalsIgnoreCase(key)) return true;
        String ep = model.getEndpoint();
        return ep != null && ep.toLowerCase().startsWith("mock://");
    }

    /**
     * Mock 响应 — 不发真请求,直接根据 provider + question 生成可重复的假答案
     */
    private ModelCallResult mockCall(ModelConfig model, String question, long start) {
        // 模拟延迟 200-800ms
        try {
            Thread.sleep(200 + (long)(Math.random() * 600));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        long latency = System.currentTimeMillis() - start;
        String content = buildMockContent(model, question);
        // 估算 token:英文按字符/4,中文按字符/2
        int inTok = Math.max(1, (int) (question.length() / 2.5));
        int outTok = Math.max(1, (int) (content.length() / 3.0));
        return ModelCallResult.ok(content, inTok, outTok, latency);
    }

    /**
     * 构造 mock 答案 — 根据 provider 不同有不同风格
     */
    private String buildMockContent(ModelConfig model, String question) {
        String provider = provider();
        String q = question == null ? "" : question;
        String lower = q.toLowerCase();
        String header = "[" + provider + " Mock 回答 · " + (model.getModelVersion() == null ? "?" : model.getModelVersion()) + "]\n\n";

        // 根据问题关键词给不同风格的回答
        if (lower.contains("代码") || lower.contains("python") || lower.contains("function") || lower.contains("写一个") || lower.contains("实现")) {
            return header + "以下是" + provider + " 提供的代码示例:\n\n"
                + "```python\n"
                + "def solution(input_data):\n"
                + "    \"\"\"根据输入数据进行处理\"\"\"\n"
                + "    result = process(input_data)\n"
                + "    return result\n"
                + "```\n\n"
                + "**说明**:这是一个标准的解决方案,使用了解析+处理+返回的范式。\n"
                + "- 优点:可读性高、易于测试\n"
                + "- 适用场景:大多数常见需求\n\n"
                + "(这是 " + provider + " 的 mock 答案,真实环境会调用真 API)";
        }
        if (lower.contains("什么是") || lower.contains("解释") || lower.contains("区别") || lower.contains("how") || lower.contains("what") || lower.contains("why")) {
            return header + "关于" + q.split("[?？]")[0] + "的问题,我的理解如下:\n\n"
                + "这是一个经典的开放性问题。从" + provider + " 视角,我认为有以下几个关键点:\n\n"
                + "1. **核心概念**:先明确问题涉及的基本概念和定义\n"
                + "2. **应用场景**:结合实际使用情况分析\n"
                + "3. **最佳实践**:给出建议的解决方案\n\n"
                + "综合来看,这个问题需要根据具体场景灵活处理。\n\n"
                + "(这是 " + provider + " 的 mock 答案,真实环境会调用真 API)";
        }
        // 通用回答
        return header + "针对您的问题:\n\n" + q + "\n\n"
            + provider + " 给出的回答是:这是一个值得深入思考的问题。"
            + "建议从以下角度进一步分析:\n"
            + "- 背景调研\n- 方案对比\n- 风险评估\n- 实施建议\n\n"
            + "(这是 " + provider + " 的 mock 答案,真实环境会调用真 API)";
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    /**
     * 从 JSON 字符串中提取指定 key 后第一个字符串 value。
     * 简单实现,只处理本项目出现的 OpenAI 兼容协议字段。
     */
    protected static String extractJsonField(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + key.length());
        if (start < 0) return null;
        start++; // 跳过开始的 "
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; continue; }
            if (c == '"') break;
            end++;
        }
        if (end >= json.length()) return null;
        String raw = json.substring(start, end);
        return raw.replace("\\n", "\n").replace("\\\"", "\"")
                  .replace("\\\\", "\\").replace("\\r", "\r").replace("\\t", "\t");
    }

    protected static Integer extractJsonInt(String json, String key) {
        if (json == null) return null;
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int p = idx + key.length();
        while (p < json.length() && (Character.isWhitespace(json.charAt(p)) || json.charAt(p) == ':')) p++;
        int start = p;
        while (p < json.length() && (Character.isDigit(json.charAt(p)) || json.charAt(p) == '-')) p++;
        if (p == start) return null;
        try {
            return Integer.parseInt(json.substring(start, p));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
