package com.mavis.evalplatform.stats.controller;

import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 占位 — FR-06 由宋子翔实现
 *
 * @author 宋子翔
 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "统计", description = "FR-06 由宋子翔实现")
public class StatsController {

    @GetMapping("/kappa")
    public Result<Map<String, Object>> kappa(@RequestParam Long evaluationId) {
        return Result.error(501, "FR-06 待宋子翔实现");
    }
}
