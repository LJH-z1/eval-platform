package com.mavis.evalplatform.model.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.common.util.AesUtil;
import com.mavis.evalplatform.model.dto.ModelConfigRequest;
import com.mavis.evalplatform.model.dto.ModelConfigVO;
import com.mavis.evalplatform.model.entity.ModelConfig;
import com.mavis.evalplatform.model.mapper.ModelConfigMapper;
import com.mavis.evalplatform.model.service.ModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型配置 Service 实现(FR-02)
 * <p>
 * 业务规则:
 * <ol>
 *   <li>API Key 入库前用 {@link AesUtil} AES-256 加密,出库时掩码</li>
 *   <li>创建时 provider + name 联合唯一(name 全局唯一)</li>
 *   <li>更新时 provider 不允许改</li>
 *   <li>删除前检查 evaluation 表是否引用;引用则抛 1023</li>
 *   <li>test() 调用 {@code ModelAdapterFactory} — FR-04 实现前用 stub,确保接口跑通</li>
 * </ol>
 *
 * @author 向锏楠
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final ModelConfigMapper modelConfigMapper;
    private final AesUtil aesUtil;
    private final JdbcTemplate jdbcTemplate;

    // ============== 查询 ==============

    @Override
    public PageResult<ModelConfigVO> page(long pageNum, long pageSize, String provider) {
        Page<ModelConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ModelConfig> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(provider)) {
            qw.eq(ModelConfig::getProvider, provider);
        }
        qw.orderByDesc(ModelConfig::getUpdatedAt);

        Page<ModelConfig> result = modelConfigMapper.selectPage(page, qw);
        List<ModelConfigVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<ModelConfigVO> listEnabled() {
        LambdaQueryWrapper<ModelConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(ModelConfig::getStatus, 1);
        qw.orderByAsc(ModelConfig::getProvider).orderByAsc(ModelConfig::getName);
        return modelConfigMapper.selectList(qw).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public ModelConfigVO getById(Long id) {
        ModelConfig e = modelConfigMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        return toVO(e);
    }

    // ============== 创建 ==============

    @Override
    @Transactional
    public ModelConfigVO create(ModelConfigRequest req) {
        req.applyDefaults();
        validate(req, true);

        // name 唯一
        if (existsByName(req.getName(), null)) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "模型名称已被占用: " + req.getName());
        }

        ModelConfig e = new ModelConfig();
        applyRequest(e, req);
        e.setApiKey(aesUtil.encrypt(req.getApiKey())); // 加密入库
        modelConfigMapper.insert(e);
        log.info("[Model] 创建 model id={} name={} provider={}", e.getId(), e.getName(), e.getProvider());
        return toVO(e);
    }

    // ============== 更新 ==============

    @Override
    @Transactional
    public ModelConfigVO update(Long id, ModelConfigRequest req) {
        ModelConfig exist = modelConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        // provider 不允许改
        if (!exist.getProvider().equals(req.getProvider())) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED,
                    "提供商(provider)不允许修改,当前=" + exist.getProvider());
        }
        // name 唯一(排除自己)
        if (!exist.getName().equals(req.getName()) && existsByName(req.getName(), id)) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "模型名称已被占用: " + req.getName());
        }

        applyRequest(exist, req);
        // 重新加密 API Key(可能用户轮换了)
        exist.setApiKey(aesUtil.encrypt(req.getApiKey()));
        modelConfigMapper.updateById(exist);
        log.info("[Model] 更新 model id={}", id);
        return toVO(exist);
    }

    // ============== 启停 ==============

    @Override
    @Transactional
    public void toggleStatus(Long id, int status) {
        ModelConfig exist = modelConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        ModelConfig patch = new ModelConfig();
        patch.setId(id);
        patch.setStatus(status);
        modelConfigMapper.updateById(patch);
        log.info("[Model] toggleStatus id={} -> {}", id, status);
    }

    // ============== 删除 ==============

    @Override
    @Transactional
    public void delete(Long id) {
        ModelConfig exist = modelConfigMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        // 检查是否被评测引用 — 用通用 SQL 探测 evaluation 表的 model_ids 字段(JSON 数组)
        // 注:FR-04 实现后,evaluation 表会有 model_ids VARCHAR/TEXT(JSON),用 LIKE '%"id":id%'
        int referenced = countReferencedByEvaluations(id);
        if (referenced > 0) {
            throw new BusinessException(ErrorCode.MODEL_REFERENCED,
                    "该模型已被 " + referenced + " 个评测任务引用,无法删除");
        }
        modelConfigMapper.deleteById(id);
        log.info("[Model] 删除 model id={}", id);
    }

    /**
     * 检查 model_id 是否出现在 evaluation 表的 model_ids 字段中。
     * <p>
     * 用 JdbcTemplate 跑原生 SQL — 因为 evaluation 是 FR-04 建的表,这里用 LIKE 兼容 JSON 字符串字段。
     * <p>
     * 如果 evaluation 表不存在(FR-04 未实现),返回 0 — 允许删除。
     */
    private int countReferencedByEvaluations(Long modelId) {
        try {
            String pattern = "%\"" + modelId + "\"%";
            Integer result = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM evaluation WHERE model_ids LIKE ?",
                    Integer.class, pattern);
            return result == null ? 0 : result;
        } catch (Exception e) {
            // evaluation 表不存在 / SQL 错 — 当作无引用,允许删除
            log.debug("[Model] evaluation 引用检查失败(表未就绪?),跳过: {}", e.getMessage());
            return 0;
        }
    }

    // ============== 连接测试 ==============

    @Override
    public ModelTestResult test(Long id, String testQuestion) {
        ModelConfig m = modelConfigMapper.selectById(id);
        if (m == null) {
            throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        }
        return doTest(m, testQuestion);
    }

    @Override
    public ModelTestResult test(ModelConfigRequest req, String testQuestion) {
        // 用于 /api/models/test 直接传 provider+apiKey 测试,不入库
        ModelConfig stub = new ModelConfig();
        stub.setProvider(req.getProvider());
        stub.setEndpoint(req.getEndpoint());
        stub.setModelVersion(req.getModelVersion());
        stub.setTemperature(req.getTemperature() != null ? req.getTemperature() : new java.math.BigDecimal("0.7"));
        stub.setTopP(req.getTopP() != null ? req.getTopP() : new java.math.BigDecimal("0.9"));
        stub.setMaxTokens(req.getMaxTokens() != null ? req.getMaxTokens() : 512);
        stub.setApiKey(aesUtil.encrypt(req.getApiKey())); // 测试模式也走加密
        return doTest(stub, testQuestion);
    }

    /**
     * 真正的"调模型"动作。
     * <p>
     * **当前实现(FR-02)**:返回 stub 响应,因为 ModelAdapterFactory 在 FR-04 才实现。
     * 后续 FR-04 完成后,把 {@code adapter.call(...)} 接进来即可。
     */
    private ModelTestResult doTest(ModelConfig m, String question) {
        long start = System.currentTimeMillis();
        try {
            // 先尝试解密 API Key(失败说明数据库里写入了非密文 — 数据问题)
            String apiKeyPlain;
            try {
                apiKeyPlain = aesUtil.decrypt(m.getApiKey());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.MODEL_API_KEY_INVALID,
                        "API Key 字段无法解密,可能数据库中存的是非加密数据");
            }
            if (!StringUtils.hasText(apiKeyPlain)) {
                throw new BusinessException(ErrorCode.MODEL_API_KEY_INVALID);
            }

            // 探测真实 API — 仅在 endpoint 看起来像真实 URL 时尝试
            // 否则 fallback 到 stub,避免启动时无 API Key 让 dev 模式跑不起来
            String responseText;
            if (StringUtils.hasText(m.getEndpoint()) && m.getEndpoint().startsWith("http")) {
                responseText = callRealApi(m, apiKeyPlain, question);
            } else {
                responseText = "[stub] 连接测试成功(provider=" + m.getProvider()
                        + ", version=" + m.getModelVersion()
                        + ", endpoint=" + m.getEndpoint() + ")";
            }
            long latency = System.currentTimeMillis() - start;
            log.info("[Model] test id/provider={} latency={}ms", m.getId() == null ? "new" : m.getId(), latency);
            return new ModelTestResult(responseText, latency, null);

        } catch (BusinessException be) {
            return new ModelTestResult(null, System.currentTimeMillis() - start, be.getMessage());
        } catch (Exception e) {
            log.error("[Model] test failed", e);
            return new ModelTestResult(null, System.currentTimeMillis() - start, e.getMessage());
        }
    }

    /**
     * 真实 API 调用(简化版 — 用 JDK HttpClient,只支持 chat/completions 类)。
     * <p>
     * 真正的多 provider 适配在 FR-04 做。当前实现:
     * <ul>
     *   <li>仅支持 OpenAI-compatible chat/completions 协议(POST {endpoint} + Authorization Bearer)</li>
     *   <li>其它协议(智谱、文心等)等到 FR-04 实现 Adapter</li>
     * </ul>
     */
    private String callRealApi(ModelConfig m, String apiKey, String question) throws Exception {
        java.net.URI uri = java.net.URI.create(m.getEndpoint());
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        String body = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"temperature\":%s,\"max_tokens\":%d}",
                m.getModelVersion() == null ? "" : m.getModelVersion().replace("\"", "\\\""),
                question == null ? "" : question.replace("\"", "\\\"").replace("\n", " "),
                m.getTemperature(),
                m.getMaxTokens()
        );

        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(java.time.Duration.ofSeconds(30))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new BusinessException(ErrorCode.MODEL_TEST_FAILED,
                    "HTTP " + resp.statusCode() + ": " + resp.body());
        }
        // 简单提取 OpenAI 格式的 content
        String respBody = resp.body();
        int idx = respBody.indexOf("\"content\":");
        if (idx >= 0) {
            int start = respBody.indexOf('"', idx + 11) + 1;
            int end = respBody.indexOf('"', start);
            if (end > start) return respBody.substring(start, end);
        }
        return respBody.length() > 500 ? respBody.substring(0, 500) + "..." : respBody;
    }

    // ============== 辅助 ==============

    private void validate(ModelConfigRequest req, boolean isCreate) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "模型名称不能为空");
        }
        if (!StringUtils.hasText(req.getProvider())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "提供商不能为空");
        }
        Set<String> validProviders = Set.of("M3", "ZHIPU", "QWEN", "WENXIN", "KIMI", "OPENAI", "CUSTOM");
        if (!validProviders.contains(req.getProvider().toUpperCase())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "不支持的提供商: " + req.getProvider() + " (允许: " + validProviders + ")");
        }
        if (!StringUtils.hasText(req.getApiKey())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "API Key 不能为空");
        }
        if (req.getTemperature() != null
                && (req.getTemperature().compareTo(java.math.BigDecimal.ZERO) < 0
                    || req.getTemperature().compareTo(java.math.BigDecimal.ONE) > 0)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "temperature 必须在 [0, 1]");
        }
        if (req.getTopP() != null
                && (req.getTopP().compareTo(java.math.BigDecimal.ZERO) < 0
                    || req.getTopP().compareTo(java.math.BigDecimal.ONE) > 0)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "topP 必须在 [0, 1]");
        }
        if (req.getMaxTokens() != null && (req.getMaxTokens() < 1 || req.getMaxTokens() > 32000)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "maxTokens 必须在 [1, 32000]");
        }
    }

    private void applyRequest(ModelConfig e, ModelConfigRequest req) {
        e.setName(req.getName());
        e.setProvider(req.getProvider().toUpperCase());
        e.setEndpoint(req.getEndpoint());
        e.setModelVersion(req.getModelVersion());
        e.setTemperature(req.getTemperature());
        e.setTopP(req.getTopP());
        e.setMaxTokens(req.getMaxTokens());
        e.setPricePerK(req.getPricePerK());
        e.setStatus(req.getStatus() == null ? 1 : req.getStatus());
    }

    private boolean existsByName(String name, Long excludeId) {
        LambdaQueryWrapper<ModelConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(ModelConfig::getName, name);
        if (excludeId != null) {
            qw.ne(ModelConfig::getId, excludeId);
        }
        return modelConfigMapper.selectCount(qw) > 0;
    }

    private ModelConfigVO toVO(ModelConfig e) {
        if (e == null) return null;
        ModelConfigVO v = new ModelConfigVO();
        v.setId(e.getId());
        v.setName(e.getName());
        v.setProvider(e.getProvider());
        // 解密后掩码
        try {
            String plain = aesUtil.decrypt(e.getApiKey());
            v.setApiKeyMasked(AesUtil.mask(plain));
        } catch (Exception ex) {
            // 数据库里写入了非密文(老数据),直接显示脱敏占位
            log.warn("[Model] apiKey 解密失败 id={}, 显示占位掩码", e.getId());
            v.setApiKeyMasked("****");
        }
        v.setEndpoint(e.getEndpoint());
        v.setModelVersion(e.getModelVersion());
        v.setTemperature(e.getTemperature());
        v.setTopP(e.getTopP());
        v.setMaxTokens(e.getMaxTokens());
        v.setPricePerK(e.getPricePerK());
        v.setStatus(e.getStatus());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }

    /**
     * 批量 ID 列表(供评测模块用)
     */
    public List<ModelConfig> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return modelConfigMapper.selectBatchIds(ids);
    }
}
