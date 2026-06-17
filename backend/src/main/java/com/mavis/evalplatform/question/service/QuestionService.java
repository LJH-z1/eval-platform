package com.mavis.evalplatform.question.service;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.question.dto.QuestionRequest;
import com.mavis.evalplatform.question.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 问题 Service — 接口契约
 * <p>
 * 由【向锏楠 FR-03】已完整实现(本分支)。
 *
 * @author 向锏楠
 */
public interface QuestionService {

    /** 单题输入(content 1-4000 字) */
    Question create(QuestionRequest req, Long userId);

    /** 更新 */
    Question update(Long id, QuestionRequest req);

    /** 软删除(deleted=1) */
    void delete(Long id);

    /** 详情 */
    Question getById(Long id);

    /** 分页/筛选(支持 category/type/difficulty/keyword 模糊) */
    PageResult<Question> page(long pageNum, long pageSize, String category, String type,
                              String difficulty, String keyword);

    /** 题库列表(公共 + 当前用户的个人) */
    List<Question> listForLibrary(Long userId);

    /** 批量导入(CSV,每行 1 题,≤ 200 题/次) */
    ImportResult importBatch(MultipartFile file);

    record ImportResult(int success, int failed, List<String> errorMessages) {}
}
