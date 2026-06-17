package com.mavis.evalplatform.question.service;

import com.mavis.evalplatform.common.result.PageResult;
import com.mavis.evalplatform.question.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 问题 Service — 接口契约
 * <p>
 * 由【向锏楠 FR-03】实现。
 *
 * @author 向锏楠
 */
public interface QuestionService {

    /** 单题输入(限 4000 字) */
    Question create(Question q);

    /** 编辑 */
    Question update(Long id, Question q);

    /** 软删除(列表不再显示,DB deleted=1) */
    void delete(Long id);

    /** 分页/筛选 */
    PageResult<Question> page(long pageNum, long pageSize, String category, String type);

    /** 公共题库 + 个人题库 */
    List<Question> listMyLibrary(Long userId);

    /** 批量导入(Excel/CSV,每行 1 题,≤ 200 题/次) */
    ImportResult importBatch(MultipartFile file);

    record ImportResult(int success, int failed, List<String> errorMessages) {}
}
