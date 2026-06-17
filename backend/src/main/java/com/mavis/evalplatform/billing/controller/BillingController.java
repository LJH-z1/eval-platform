package com.mavis.evalplatform.billing.controller;

import com.mavis.evalplatform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 占位 — FR-07 由梁倩倩实现
 * <p>
 * 真正的实现需要 @RequiredArgsConstructor 注入 BillingService。
 * 这里先放个返回 501 的占位接口,让 Spring 上下文能起来。
 *
 * @author 梁倩倩
 */
@RestController
@RequestMapping("/api/billing")
@Tag(name = "成本统计", description = "FR-07 由梁倩倩实现")
public class BillingController {

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam Long evaluationId) {
        return Result.error(501, "FR-07 待梁倩倩实现,当前仅占位");
    }
}
