package com.mavis.evalplatform.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 问题创建/更新请求
 *
 * @author 向锏楠(已由本分支完整实现)
 */
@Data
@Schema(description = "问题创建/更新请求")
public class QuestionRequest implements Serializable {

    /** 更新时必填 */
    @Schema(description = "更新时必填")
    private Long id;

    @NotBlank(message = "问题内容不能为空")
    @Size(min = 1, max = 4000, message = "问题长度需在 1-4000 字")
    @Schema(description = "问题内容", example = "什么是光合作用?")
    private String content;

    @Schema(description = "学科分类", example = "科学")
    private String category;

    @Schema(description = "难度:简单/中等/困难", example = "中等")
    private String difficulty;

    @Schema(description = "题型:事实/推理/创作/代码", example = "事实")
    private String type;

    @Schema(description = "期望答案(可选)")
    private String expectedAnswer;

    @Schema(description = "是否公共题库(默认 false=个人题库)", example = "false")
    private Boolean isPublic;
}
