package com.mavis.evalplatform.evaluation.controller;

import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 占位 — FR-04 由梁倩倩实现
 *
 * @author 梁倩倩
 */
@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "评测", description = "FR-04 由梁倩倩实现")
public class EvaluationController {

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return Result.error(501, "FR-04 待梁倩倩实现");
    }
}
