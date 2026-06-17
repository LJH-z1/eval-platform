package com.mavis.evalplatform.model.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.model.entity.ModelConfig;

import java.util.List;

/**
 * 模型配置 Service — 接口契约
 * <p>
 * 由【向锏楠 FR-02】实现具体业务。
 * <p>
 * 业务规则(对齐需求规格说明书 §3.2.4):
 * <ul>
 *   <li>API Key 加密存储(AES-256),用 {@code common.util.AesUtil}</li>
 *   <li>同一提供商可配置多个模型版本</li>
 *   <li>被引用的模型不允许删除(评测中已使用 → 抛 1001 业务错误)</li>
 *   <li>连接测试:输入测试问题,验证 API 可用性,记录耗时</li>
 * </ul>
 *
 * @author 向锏楠
 */
public interface ModelService {

    /**
     * 列表/分页查询
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param provider 可选,按提供商过滤
     * @return 分页结果
     */
    PageResult<ModelConfig> page(long pageNum, long pageSize, String provider);

    /**
     * 获取已启用的模型(供评测页下拉用)
     */
    List<ModelConfig> listEnabled();

    /**
     * 新增模型
     */
    ModelConfig create(ModelConfig model);

    /**
     * 更新模型(不允许改 provider,可改 endpoint/参数)
     */
    ModelConfig update(Long id, ModelConfig model);

    /**
     * 启用/停用
     */
    void toggleStatus(Long id, int status);

    /**
     * 删除(被引用则抛业务异常)
     */
    void delete(Long id);

    /**
     * 连接测试
     * @param id 模型 ID
     * @param testQuestion 测试问题
     * @return 模型回答与耗时
     */
    ModelTestResult test(Long id, String testQuestion);

    /**
     * 连接测试结果
     */
    record ModelTestResult(String response, long latencyMs, String error) {}
}
