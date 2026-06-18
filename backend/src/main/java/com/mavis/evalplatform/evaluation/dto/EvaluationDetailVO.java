package com.mavis.evalplatform.evaluation.dto;

import com.mavis.evalplatform.evaluation.entity.Answer;
import com.mavis.evalplatform.evaluation.entity.Evaluation;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评测详情 VO(包含 answer 列表)
 *
 * @author 梁倩倩
 */
@Data
public class EvaluationDetailVO {

    private Long id;
    private String name;
    private String description;
    private String status;
    private Long createdBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    private List<Long> modelIds;
    private List<Long> questionIds;
    private List<Answer> answers;

    public static EvaluationDetailVO from(Evaluation e, List<Answer> answers) {
        EvaluationDetailVO v = new EvaluationDetailVO();
        v.id = e.getId();
        v.name = e.getName();
        v.description = e.getDescription();
        v.status = e.getStatus();
        v.createdBy = e.getCreatedBy();
        v.startedAt = e.getStartedAt();
        v.finishedAt = e.getFinishedAt();
        v.createdAt = e.getCreatedAt();
        // modelIds / questionIds 字段在 entity 中是逗号分隔字符串,转 List 方便前端
        v.modelIds = parseIds(e.getModelIds());
        v.questionIds = parseIds(e.getQuestionIds());
        v.answers = answers;
        return v;
    }

    private static List<Long> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        try {
            return java.util.Arrays.stream(csv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }
}
