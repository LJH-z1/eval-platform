package com.mavis.evalplatform.evaluation.adapter;

import org.springframework.stereotype.Component;

/**
 * 月之暗面 Kimi 适配器 — OpenAI 兼容
 *
 * @author 向锏楠
 */
@Component
public class KimiAdapter extends OpenAiCompatibleAdapter {

    public static final String PROVIDER = "KIMI";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    protected String defaultEndpoint() {
        return "https://api.moonshot.cn/v1/chat/completions";
    }

    @Override
    protected String defaultModelVersion() {
        return "moonshot-v1-8k";
    }
}
