package com.mavis.evalplatform.evaluation.adapter;

import org.springframework.stereotype.Component;

/**
 * OpenAI 适配器 — 标准 OpenAI 协议
 *
 * @author 向锏楠
 */
@Component
public class OpenAiAdapter extends OpenAiCompatibleAdapter {

    public static final String PROVIDER = "OPENAI";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    protected String defaultEndpoint() {
        return "https://api.openai.com/v1/chat/completions";
    }

    @Override
    protected String defaultModelVersion() {
        return "gpt-4o-mini";
    }
}
