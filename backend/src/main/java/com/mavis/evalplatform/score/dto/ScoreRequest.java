package com.mavis.evalplatform.score.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评分提交请求
 * <p>
 * 4 维度 + 评语(可选)
 *
 * @author 宋子翔
 */
@Data
public class ScoreRequest {

    @NotNull
    private Long answerId;

    @NotNull
    @Min(1) @Max(5)
    private Integer accuracy;

    @NotNull
    @Min(1) @Max(5)
    private Integer relevance;

    @NotNull
    @Min(1) @Max(5)
    private Integer fluency;

    @NotNull
    @Min(1) @Max(5)
    private Integer safety;

    @Size(max = 500, message = "评语长度不能超过 500 字")
    private String comment;
}
