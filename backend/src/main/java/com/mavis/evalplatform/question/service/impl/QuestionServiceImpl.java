package com.mavis.evalplatform.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mavis.evalplatform.common.exception.BusinessException;
import com.mavis.evalplatform.common.exception.ErrorCode;
import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.question.dto.QuestionRequest;
import com.mavis.evalplatform.question.entity.Question;
import com.mavis.evalplatform.question.mapper.QuestionMapper;
import com.mavis.evalplatform.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 问题 Service 实现 — FR-03
 * <p>
 * 业务规则(对齐需求规格说明书 §3.3.4):
 * <ul>
 *   <li>单题 content 长度 1-4000 字</li>
 *   <li>批量导入每行 1 题,≤ 200 题/次</li>
 *   <li>软删除(deleted=1)保留历史引用</li>
 * </ul>
 *
 * @author 向锏楠(已由本分支完整实现)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    /** 合法 difficulty 取值 */
    private static final List<String> DIFFICULTIES = Arrays.asList("简单", "中等", "困难");
    /** 合法 type 取值 */
    private static final List<String> TYPES = Arrays.asList("事实", "推理", "创作", "代码");

    private final QuestionMapper questionMapper;

    @Override
    @Transactional
    public Question create(QuestionRequest req, Long userId) {
        validate(req);
        Question q = new Question();
        applyRequest(q, req, userId);
        questionMapper.insert(q);
        log.info("[Question] create id={}, user={}, content.len={}", q.getId(), userId,
                q.getContent() == null ? 0 : q.getContent().length());
        return q;
    }

    @Override
    @Transactional
    public Question update(Long id, QuestionRequest req) {
        validate(req);
        Question exist = questionMapper.selectById(id);
        if (exist == null || (exist.getDeleted() != null && exist.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        applyRequest(exist, req, exist.getCreatedBy());
        questionMapper.updateById(exist);
        log.info("[Question] update id={}", id);
        return exist;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        // 用普通 wrapper 强制更新 deleted,绕过 MyBatis-Plus 逻辑删除的拦截
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Question> w =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        w.eq("id", id)
         .set("deleted", 1)
         .set("updated_at", LocalDateTime.now());
        questionMapper.update(null, w);
        log.info("[Question] soft-delete id={}", id);
    }

    @Override
    public Question getById(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null || (q.getDeleted() != null && q.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        return q;
    }

    @Override
    public PageResult<Question> page(long pageNum, long pageSize, String category, String type,
                                      String difficulty, String keyword) {
        Page<Question> page = Page.of(pageNum, pageSize);
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<>();
        w.eq(Question::getDeleted, 0);
        if (StringUtils.hasText(category))    w.eq(Question::getCategory, category);
        if (StringUtils.hasText(type))         w.eq(Question::getType, type);
        if (StringUtils.hasText(difficulty))   w.eq(Question::getDifficulty, difficulty);
        if (StringUtils.hasText(keyword)) {
            w.and(qw -> qw.like(Question::getContent, keyword)
                    .or().like(Question::getExpectedAnswer, keyword));
        }
        w.orderByDesc(Question::getCreatedAt);
        Page<Question> result = questionMapper.selectPage(page, w);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<Question> listForLibrary(Long userId) {
        LambdaQueryWrapper<Question> w = new LambdaQueryWrapper<>();
        w.eq(Question::getDeleted, 0);
        w.orderByDesc(Question::getCreatedAt);
        w.last("LIMIT 200");
        return questionMapper.selectList(w);
    }

    @Override
    @Transactional
    public ImportResult importBatch(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件不能为空");
        }
        int[] counters = new int[]{0, 0};  // [success, failed]
        List<String> errors = new ArrayList<>();
        int total = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            boolean isFirst = true;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (isFirst) {
                    isFirst = false;
                    if (line.toLowerCase().contains("content") || line.contains("题")) {
                        continue;
                    }
                }
                if (total >= 200) {
                    errors.add("超过单次导入 200 题上限,已截断");
                    break;
                }
                if (line.trim().isEmpty()) continue;
                String[] cells = parseCsvLine(line);
                if (cells.length == 0) continue;
                try {
                    String content = cells[0].trim();
                    if (content.isEmpty()) {
                        counters[1]++;
                        errors.add("第 " + lineNo + " 行:内容为空");
                        continue;
                    }
                    if (content.length() > 4000) {
                        counters[1]++;
                        errors.add("第 " + lineNo + " 行:内容超过 4000 字");
                        continue;
                    }
                    Question q = new Question();
                    q.setContent(content);
                    q.setCategory(cells.length > 1 ? cells[1].trim() : null);
                    q.setDifficulty(cells.length > 2 ? cells[2].trim() : "中等");
                    q.setType(cells.length > 3 ? cells[3].trim() : "事实");
                    q.setExpectedAnswer(cells.length > 4 ? cells[4].trim() : null);
                    q.setDeleted(0);
                    q.setCreatedBy(null);
                    q.setCreatedAt(LocalDateTime.now());
                    q.setUpdatedAt(LocalDateTime.now());
                    questionMapper.insert(q);
                    counters[0]++;
                    total++;
                } catch (Exception e) {
                    counters[1]++;
                    errors.add("第 " + lineNo + " 行:" + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "读取文件失败:" + e.getMessage());
        }
        log.info("[Question] import total={}, success={}, failed={}", total, counters[0], counters[1]);
        return new ImportResult(counters[0], counters[1], errors);
    }

    // -------- 私有方法 --------

    private void validate(QuestionRequest req) {
        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "问题内容不能为空");
        }
        if (req.getContent().length() > 4000) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "问题内容不能超过 4000 字");
        }
        if (StringUtils.hasText(req.getDifficulty()) && !DIFFICULTIES.contains(req.getDifficulty())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "难度取值必须为 简单/中等/困难");
        }
        if (StringUtils.hasText(req.getType()) && !TYPES.contains(req.getType())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "题型取值必须为 事实/推理/创作/代码");
        }
    }

    private void applyRequest(Question q, QuestionRequest req, Long createdBy) {
        q.setContent(req.getContent().trim());
        q.setCategory(StringUtils.hasText(req.getCategory()) ? req.getCategory().trim() : null);
        q.setDifficulty(StringUtils.hasText(req.getDifficulty()) ? req.getDifficulty() : null);
        q.setType(StringUtils.hasText(req.getType()) ? req.getType() : null);
        q.setExpectedAnswer(StringUtils.hasText(req.getExpectedAnswer())
                ? req.getExpectedAnswer().trim() : null);
        q.setIsPublic(Boolean.TRUE.equals(req.getIsPublic()) ? 1 : 0);
        if (q.getId() == null) {
            q.setCreatedBy(createdBy);
            q.setCreatedAt(LocalDateTime.now());
        }
        q.setUpdatedAt(LocalDateTime.now());
    }

    /** 简易 CSV 解析:支持双引号包围的字段含逗号 */
    private String[] parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                cells.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        cells.add(cur.toString());
        return cells.toArray(new String[0]);
    }
}
