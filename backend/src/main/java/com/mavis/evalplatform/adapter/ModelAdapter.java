package com.mavis.evalplatform.adapter;

import java.math.BigDecimal;

/**
 * 模型适配器统一接口 — 核心抽象
 * <p>
 * 对齐架构设计说明书 §4.2.2 / §7.1 适配器模式
 * <p>
 * 屏蔽各 LLM API 协议差异,对业务层提供统一接口。
 * <p>
 * 由【向锏楠(适配器实现)+ 宋子翔(接口设计)】完成,5 个具体实现:
 * <ul>
 *   <li>{@code M3Adapter} — MiniMax M3(OpenAI 兼容协议)</li>
 *   <li>{@code ZhipuAdapter} — 智谱 GLM-4(自有协议)</li>
 *   <li>{@code QwenAdapter} — 通义千问(OpenAI 兼容协议)</li>
 *   <li>{@code WenxinAdapter} — 文心一言(Access Token)</li>
 *   <li>{@code KimiAdapter} — 月之暗面 Kimi(OpenAI 兼容协议)</li>
 * </ul>
 * <p>
 * 新增模型只需新增一个 Adapter,不影响业务层和其他适配器。
 *
 * @author 向锏楠 / 宋子翔
 */
public interface ModelAdapter {

    /** 模型提供商标识:M3/ZHIPU/QWEN/WENXIN/KIMI */
    String provider();

    /** 单价(每千 Token,元) */
    BigDecimal pricePerKToken();

    /** 健康检查 */
    boolean healthCheck();

    /** 同步调用 */
    ModelResponse call(ModelRequest request);

    /** 流式调用 */
    reactor.core.publisher.Flux<String> stream(ModelRequest request);

    // -------- 通用 DTO(由本接口持有,所有适配器共用) --------

    record ModelRequest(String model, String apiKey, String endpoint,
                        String prompt, Double temperature, Double topP,
                        Integer maxTokens) {}

    record ModelResponse(String content, Integer promptTokens, Integer completionTokens,
                         long latencyMs, String errorCode, String errorMessage) {}
}
