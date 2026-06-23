package com.mavis.evalplatform.evaluation.adapter;

import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型适配器工厂 — Spring 收集所有 {@code @Component ModelAdapter},按 provider 分发
 *
 * @author 梁倩倩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelAdapterFactory {

    private final List<ModelAdapter> adapters;
    private final Map<String, ModelAdapter> registry = new HashMap<>();

    @PostConstruct
    public void init() {
        for (ModelAdapter a : adapters) {
            registry.put(a.provider().toUpperCase(), a);
            log.info("[ModelAdapter] registered provider={}", a.provider());
        }
    }

    /**
     * 根据 provider 名称获取适配器
     * <p>
     * CUSTOM 兜底走 OpenAI 兼容协议
     */
    public ModelAdapter getAdapter(String provider) {
        if (provider == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NO_PROVIDER_ADAPTER, "provider 为空");
        }
        String key = provider.toUpperCase();
        ModelAdapter a = registry.get(key);
        if (a == null) {
            // CUSTOM 兜底走 OpenAI 兼容
            if ("CUSTOM".equals(key)) {
                a = registry.get("OPENAI");
            }
        }
        if (a == null) {
            throw new BusinessException(ErrorCode.EVALUATION_NO_PROVIDER_ADAPTER,
                    "未找到 provider=" + provider + " 的适配器,已注册: " + registry.keySet());
        }
        return a;
    }

    /**
     * 已注册的 provider 列表
     */
    public List<String> registeredProviders() {
        return List.copyOf(registry.keySet());
    }
}
