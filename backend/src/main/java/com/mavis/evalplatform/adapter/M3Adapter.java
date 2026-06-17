package com.mavis.evalplatform.adapter;

import org.springframework.stereotype.Component;

/**
 * MiniMax M3 适配器 — 接口骨架
 * <p>
 * M3 使用 OpenAI 兼容协议,可以参考 {@code openai-java} 的调用方式。
 * <p>
 * 由【向锏楠】实现具体 HTTP 调用。
 *
 * @author 向锏楠
 */
@Component
public class M3Adapter implements ModelAdapter {

    @Override public String provider() { return "M3"; }

    @Override public java.math.BigDecimal pricePerKToken() { return java.math.BigDecimal.ZERO; }

    @Override public boolean healthCheck() { throw new UnsupportedOperationException("TODO 向锏楠 FR-04/adapter 实现 M3Adapter.healthCheck"); }

    @Override public ModelResponse call(ModelRequest request) {
        throw new UnsupportedOperationException("TODO 向锏楠 FR-04/adapter 实现 M3Adapter.call");
    }

    @Override public reactor.core.publisher.Flux<String> stream(ModelRequest request) {
        throw new UnsupportedOperationException("TODO 向锏楠 FR-04/adapter 实现 M3Adapter.stream");
    }
}
