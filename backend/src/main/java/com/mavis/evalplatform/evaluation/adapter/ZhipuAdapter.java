package com.mavis.evalplatform.evaluation.adapter;

import org.springframework.stereotype.Component;

/**
 * 智谱 GLM 适配器 — OpenAI 兼容
 *
 * @author 向锏楠
 */
@Component
public class ZhipuAdapter extends OpenAiCompatibleAdapter {

    public static final String PROVIDER = "ZHIPU";

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    protected String defaultEndpoint() {
        return "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    }

    @Override
    protected String defaultModelVersion() {
        return "glm-4-plus";
    }
}
