package com.mavis.evalplatform.model.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.model.dto.ModelConfigRequest;
import com.mavis.evalplatform.model.dto.ModelConfigVO;
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
 *   <li>被引用的模型不允许删除(评测中已使用 → 抛 1023 MODEL_REFERENCED)</li>
 *   <li>连接测试:输入测试问题,验证 API 可用性,记录耗时</li>
 * </ul>
 *
 * @author 向锏楠
 */
public interface ModelService {

    /**
     * 列表/分页查询
     */
    PageResult<ModelConfigVO> page(long pageNum, long pageSize, String provider);

    /**
     * 获取已启用的模型(供评测页下拉用)
     */
    List<ModelConfigVO> listEnabled();

    /**
     * 按 ID 获取详情
     */
    ModelConfigVO getById(Long id);

    /**
     * 新增模型(自动加密 API Key)
     */
    ModelConfigVO create(ModelConfigRequest req);

    /**
     * 更新模型(不允许改 provider,可改 endpoint/参数,API Key 重新加密)
     */
    ModelConfigVO update(Long id, ModelConfigRequest req);

    /**
     * 启用/停用
     */
    void toggleStatus(Long id, int status);

    /**
     * 删除(被引用则抛 1023)
     */
    void delete(Long id);

    /**
     * 连接测试 — 用已存在模型的配置
     */
    ModelTestResult test(Long id, String testQuestion);

    /**
     * 连接测试 — 临时传入 provider + apiKey,不入库
     */
    ModelTestResult test(ModelConfigRequest req, String testQuestion);

    /**
     * 连接测试结果
     */
    record ModelTestResult(String response, long latencyMs, String error) {}

    /**
     * 批量按 ID 查询(供评测模块引用 — 入参列表,出参实体含加密 apiKey,慎用)
     */
    List<ModelConfig> listByIds(List<Long> ids);
}
