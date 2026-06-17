package com.mavis.evalplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 多模型回答对比与评测平台 启动类
 * <p>
 * 模块负责人:刘家豪 (组长/后端主程)
 * 负责模块:FR-01 用户登录与权限管理、common 包、config 包
 *
 * @author 刘家豪
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.mavis.evalplatform.**.mapper")
@SpringBootApplication
public class EvalPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvalPlatformApplication.class, args);
    }
}
