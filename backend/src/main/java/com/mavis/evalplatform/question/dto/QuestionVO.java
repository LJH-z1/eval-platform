package com.mavis.evalplatform.question.dto;

import com.mavis.evalplatform.question.entity.Question;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 问题响应(列表/详情用)
 *
 * @author 向锏楠
 */
@Data
@Schema(description = "问题详情")
public class QuestionVO implements Serializable {

    @Schema(description = "问题 ID")
    private Long id;

    @Schema(description = "问题内容")
    private String content;

    @Schema(description = "学科分类")
    private String category;

    @Schema(description = "难度")
    private String difficulty;

    @Schema(description = "题型")
    private String type;

    @Schema(description = "期望答案")
    private String expectedAnswer;

    @Schema(description = "创建人 ID")
    private Long createdBy;

    @Schema(description = "是否公共")
    private Boolean isPublic;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public static QuestionVO from(Question q) {
        if (q == null) return null;
        QuestionVO v = new QuestionVO();
        v.setId(q.getId());
        v.setContent(q.getContent());
        v.setCategory(q.getCategory());
        v.setDifficulty(q.getDifficulty());
        v.setType(q.getType());
        v.setExpectedAnswer(q.getExpectedAnswer());
        v.setCreatedBy(q.getCreatedBy());
        v.setIsPublic(q.getIsPublic() != null && q.getIsPublic() == 1);
        v.setCreatedAt(q.getCreatedAt());
        v.setUpdatedAt(q.getUpdatedAt());
        return v;
    }
}
