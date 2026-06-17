package com.mavis.evalplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务 / 线程池配置
 * <p>
 * - evaluationExecutor:多模型并行调用
 * - auditExecutor:异步写审计日志
 *
 * @author 刘家豪
 */
@Configuration
@EnableAsync
public class ExecutorConfig {

    @Bean("evaluationExecutor")
    public Executor evaluationExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(8);
        e.setMaxPoolSize(16);
        e.setQueueCapacity(100);
        e.setThreadNamePrefix("eval-");
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.setWaitForTasksToCompleteOnShutdown(true);
        e.setAwaitTerminationSeconds(30);
        e.initialize();
        return e;
    }

    @Bean("auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor e = new ThreadPoolTaskExecutor();
        e.setCorePoolSize(2);
        e.setMaxPoolSize(4);
        e.setQueueCapacity(500);
        e.setThreadNamePrefix("audit-");
        e.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        e.initialize();
        return e;
    }
}
