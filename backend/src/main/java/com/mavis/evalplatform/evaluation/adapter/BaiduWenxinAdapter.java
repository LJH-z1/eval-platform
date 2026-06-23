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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 百度千帆适配器 — 走 OAuth 2.0 client_credentials 拿 access_token,然后用 OpenAI 兼容格式调 chat
 * <p>
 * 鉴权流程:
 * <ol>
 *   <li>apiKey 格式:bce-v3/AK/SK(如 bce-v3/ALTAK-xxx/yyy)</li>
 *   <li>POST {endpoint}/oauth/2.0/token?grant_type=client_credentials&client_id=AK&client_secret=SK
 *       → 拿 access_token(有效期 30 天,缓存 25 分钟)</li>
 *   <li>POST {endpoint}/rpc/2.0/ai/v3/eb/chat/completions_qianfan?access_token={token}
 *       Body: OpenAI 兼容 {model, messages, temperature, max_tokens}</li>
 * </ol>
 *
 * @author 刘家豪
 */
@Slf4j
@Component
public class BaiduWenxinAdapter implements ModelAdapter {

    public static final String PROVIDER = "WENXIN";
    private static final String DEFAULT_HOST = "https://aip.baidubce.com";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Pattern TOKEN_PAT = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

    // 模型 ID → (token, expiresAt)
    private static final ConcurrentHashMap<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();
    private static final long TOKEN_TTL_MS = 25 * 60 * 1000L;

    private record CachedToken(String token, long expiresAt) {
        boolean valid() { return System.currentTimeMillis() < expiresAt; }
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public ModelCallResult call(ModelConfig model, String question) {
        long start = System.currentTimeMillis();
        if (model.getApiKey() == null || model.getApiKey().isBlank()) {
            return ModelCallResult.fail("MISSING_API_KEY", "未配置 API Key(格式 bce-v3/AK/SK)", System.currentTimeMillis() - start);
        }
        // 1) 拆 AK / SK
        String[] parts = model.getApiKey().split("/");
        if (parts.length < 3 || !"bce-v3".equalsIgnoreCase(parts[0])) {
            return ModelCallResult.fail("INVALID_API_KEY", "百度 API Key 格式错误,需 bce-v3/AK/SK", System.currentTimeMillis() - start);
        }
        String ak = parts[1];
        String sk = parts[2];
        try {
            // 2) 拿 access_token(缓存)
            String token = getAccessToken(model.getId(), ak, sk);
            // 3) 拼 chat 端点
            String base = (model.getEndpoint() == null || model.getEndpoint().isBlank()) ? DEFAULT_HOST : model.getEndpoint();
            String url = base + "/rpc/2.0/ai/v3/eb/chat/completions_qianfan?access_token=" + token;
            // 4) body
            String body = "{\"model\":" + jsonString(model.getModelVersion() == null ? "ernie-4.5-8k" : model.getModelVersion())
                    + ",\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(question) + "}]"
                    + ",\"temperature\":" + (model.getTemperature() == null ? 0.7 : model.getTemperature()) + "}";
            // 5) 请求
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
                        "千帆 " + resp.statusCode() + ": " + truncate(rb, 300), ms);
            }
            // 6) 提 content(OpenAI 兼容格式)
            String text = extractContent(rb);
            if (text == null) {
                return ModelCallResult.fail("PARSE_ERROR", "无法解析千帆响应: " + truncate(rb, 200), ms);
            }
            return ModelCallResult.ok(text, 0, 0, ms);
        } catch (Exception ex) {
            log.error("[Wenxin] call failed for model {}", model.getId(), ex);
            return ModelCallResult.fail("EXCEPTION", ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }

    private static String getAccessToken(Long modelId, String ak, String sk) throws Exception {
        String key = modelId + ":" + ak;
        CachedToken cached = TOKEN_CACHE.get(key);
        if (cached != null && cached.valid()) return cached.token;
        // 拿新 token
        String url = DEFAULT_HOST + "/oauth/2.0/token?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(ak, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(sk, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("OAuth 失败 " + resp.statusCode() + ": " + truncate(resp.body(), 200));
        }
        Matcher m = TOKEN_PAT.matcher(resp.body());
        if (!m.find()) throw new RuntimeException("OAuth 响应无 access_token: " + truncate(resp.body(), 200));
        String token = m.group(1);
        TOKEN_CACHE.put(key, new CachedToken(token, System.currentTimeMillis() + TOKEN_TTL_MS));
        return token;
    }

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

    private static String extractContent(String body) {
        int idx = body.indexOf("\"content\":\"");
        if (idx < 0) return null;
        int start = idx + 11;
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
