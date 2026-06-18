package com.mavis.evalplatform.evaluation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评测创建请求
 *
 * @author 梁倩倩
 */
@Data
public class EvaluationRequest {

    @NotNull
    @Size(min = 1, max = 100, message = "评测名称长度 1-100")
    private String name;

    @Size(max = 500, message = "描述最长 500 字")
    private String description;

    @NotEmpty(message = "至少选择 1 个模型")
    private List<Long> modelIds;

    @NotEmpty(message = "至少选择 1 个问题")
    private List<Long> questionIds;
}
