package com.mavis.evalplatform.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型适配器工厂 — 接口契约
 * <p>
 * 由【向锏楠】实现,Spring 启动时收集所有 {@code @Component} 的 ModelAdapter,
 * 业务层通过 {@code getAdapter(provider)} 拿到具体实现。
 *
 * @author 向锏楠
 */
public interface ModelAdapterFactory {

    /**
     * 根据 provider 拿适配器
     * @param provider M3/ZHIPU/QWEN/WENXIN/KIMI
     * @return 对应适配器,找不到抛 1020 DATA_NOT_FOUND
     */
    ModelAdapter getAdapter(String provider);

    /** 列出所有已注册的适配器 */
    List<String> listProviders();

    /** 工厂内部 Bean(由 Spring 自动填充) */
    @SuppressWarnings("unused")
    class Holder {
        private static final Map<String, ModelAdapter> ADAPTERS = new HashMap<>();
    }
}
