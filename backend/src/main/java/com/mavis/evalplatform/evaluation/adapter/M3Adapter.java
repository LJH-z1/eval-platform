package com.mavis.evalplatform.evaluation.adapter;

import org.springframework.stereotype.Component;

/**
 * M3(MiniMax) 适配器 — OpenAI 兼容协议
 * <p>
 * Endpoint: https://api.minimaxi.com/v1/text/chatcompletion_v2(原 api.MiniMax.chat 已重定向)
 *
 * @author 向锏楠
 */
@Component
public class M3Adapter extends OpenAiCompatibleAdapter {

    public static final String PROVIDER = "M3";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    protected String defaultEndpoint() {
        return "https://api.minimaxi.com/v1/text/chatcompletion_v2";
    }

    @Override
    protected String defaultModelVersion() {
        return "MiniMax-Text-01";
    }
}
