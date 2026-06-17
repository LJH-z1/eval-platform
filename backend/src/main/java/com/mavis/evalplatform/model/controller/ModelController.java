package com.mavis.evalplatform.model.controller;

import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 占位 — FR-02 由向锏楠实现
 *
 * @author 向锏楠
 */
@RestController
@RequestMapping("/api/models")
@Tag(name = "模型配置", description = "FR-02 由向锏楠实现")
public class ModelController {

    @GetMapping
    public Result<Map<String, Object>> page() {
        return Result.error(501, "FR-02 待向锏楠实现");
    }
}
