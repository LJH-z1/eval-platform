package com.mavis.evalplatform.score.controller;

import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 占位 — FR-05 由宋子翔实现
 *
 * @author 宋子翔
 */
@RestController
@RequestMapping("/api/scores")
@Tag(name = "评分", description = "FR-05 由宋子翔实现")
public class ScoreController {

    @PostMapping
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        return Result.error(501, "FR-05 待宋子翔实现");
    }
}
