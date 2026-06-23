package com.mavis.evalplatform.evaluation.adapter;

import com.mavis.evalplatform.model.entity.ModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Google Gemini 适配器 — 不是 OpenAI 兼容协议,需自定义实现
 * <p>
 * 协议:
 * <ul>
 *   <li>POST {endpoint}?key={apiKey} (query string 鉴权,不是 Bearer)</li>
 *   <li>Body: { contents: [{ parts: [{ text: prompt }] }] }</li>
 *   <li>Response: { candidates: [{ content: { parts: [{ text: "..." }] } }] }</li>
 * </ul>
 * <p>
 * 默认 endpoint: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 *
 * @author 刘家豪
 */
@Slf4j
@Component
public class GeminiAdapter implements ModelAdapter {

    public static final String PROVIDER = "GEMINI";
    private static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public ModelCallResult call(ModelConfig model, String question) {
        long start = System.currentTimeMillis();
        if (model.getApiKey() == null || model.getApiKey().isBlank()) {
            return ModelCallResult.fail("MISSING_API_KEY", "未配置 API Key", System.currentTimeMillis() - start);
        }
        try {
            // 1) 端点拼接:用户填 endpoint 一般是默认的,后面拼 modelVersion + :generateContent
            // 约定:用户填的 endpoint 必须以 / 结尾;若 modelVersion 为空则用 gemini-2.5-flash
            String base = model.getEndpoint();
            if (base == null || base.isBlank()) base = DEFAULT_ENDPOINT;
            if (!base.endsWith("/")) base = base + "/";
            String modelName = (model.getModelVersion() == null || model.getModelVersion().isBlank())
                    ? "gemini-2.5-flash" : model.getModelVersion();
            // endpoint 是 base/models/ 形式,拼成 base/{model}:generateContent
            String url = base + URLEncoder.encode(modelName, StandardCharsets.UTF_8) + ":generateContent"
                    + "?key=" + URLEncoder.encode(model.getApiKey(), StandardCharsets.UTF_8);

            // 2) body
            String body = "{\"contents\":[{\"parts\":[{\"text\":" + jsonString(question) + "}]}]}";

            // 3) 请求
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long ms = System.currentTimeMillis() - start;
            String rb = resp.body();
            if (resp.statusCode() / 100 != 2) {
                return ModelCallResult.fail("HTTP_" + resp.statusCode(),
                        "Gemini " + resp.statusCode() + ": " + truncate(rb, 300), ms);
            }
            // 4) 解析 — 用最简单 regex 提 text(避免引 Jackson)
            String text = extractText(rb);
            if (text == null) {
                return ModelCallResult.fail("PARSE_ERROR", "无法解析 Gemini 响应: " + truncate(rb, 200), ms);
            }
            return ModelCallResult.ok(text, 0, 0, ms);
        } catch (Exception ex) {
            log.error("[Gemini] call failed for model {}", model.getId(), ex);
            return ModelCallResult.fail("EXCEPTION", ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }

    /** 简单 JSON 字符串字面量 */
    private static String jsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** 从 candidates[0].content.parts[*].text 提第一个 text 字段 */
    private static String extractText(String body) {
        // 找 "text":"..." 第一次出现
        int idx = body.indexOf("\"text\":\"");
        if (idx < 0) return null;
        int start = idx + 8;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                char n = body.charAt(i + 1);
                switch (n) {
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case '"': sb.append('"');  i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/');  i++; break;
                    default: sb.append(c);
                }
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }
}
