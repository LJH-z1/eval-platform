package com.mavis.evalplatform.evaluation.adapter;

import com.mavis.evalplatform.model.entity.ModelConfig;

/**
 * 模型适配器 — 把各家 LLM API 统一成同一个调用接口
 * <p>
 * 由 FR-04(梁倩倩主写,向锏楠协助)实现。
 * <p>
 * 当前实现策略:
 * <ul>
 *   <li>所有提供商均实现为 OpenAI-compatible chat/completions 协议</li>
 *   <li>差异仅在 endpoint / modelVersion 字段,逻辑由 {@link OpenAiCompatibleAdapter} 统一处理</li>
 *   <li>WENXIN(文心)走 OAuth 鉴权,实现特殊一点(暂未启用,占位)</li>
 * </ul>
 *
 * @author 梁倩倩 / 向锏楠
 */
public interface ModelAdapter {

    /**
     * @return 此适配器支持的 provider 名称(大写)
     */
    String provider();

    /**
     * 调用模型
     *
     * @param model    模型配置(已解密,含明文 apiKey)
     * @param question 用户问题
     * @return 调用结果
     */
    ModelCallResult call(ModelConfig model, String question);

    /**
     * 调用结果
     */
    record ModelCallResult(
            /** 模型回答内容 */
            String content,
            /** 输入 token 估算(若 API 返回) */
            Integer tokenInput,
            /** 输出 token 估算 */
            Integer tokenOutput,
            /** 实际调用耗时(毫秒) */
            long latencyMs,
            /** 是否成功 */
            boolean success,
            /** 失败时的错误码(成功时为 null) */
            String errorCode,
            /** 失败时的错误消息 */
            String errorMessage
    ) {
        public static ModelCallResult ok(String content, int in, int out, long ms) {
            return new ModelCallResult(content, in, out, ms, true, null, null);
        }
        public static ModelCallResult fail(String code, String msg, long ms) {
            return new ModelCallResult(null, null, null, ms, false, code, msg);
        }
    }
}
