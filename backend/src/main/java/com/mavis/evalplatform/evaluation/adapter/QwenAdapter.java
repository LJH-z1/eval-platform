package com.mavis.evalplatform.evaluation.adapter;

import org.springframework.stereotype.Component;

/**
 * 通义千问(QWEN)适配器 — OpenAI 兼容(DashScope)
 *
 * @author 向锏楠
 */
@Component
public class QwenAdapter extends OpenAiCompatibleAdapter {

    public static final String PROVIDER = "QWEN";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    protected String defaultEndpoint() {
        return "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    }

    @Override
    protected String defaultModelVersion() {
        return "qwen-max";
    }
}
