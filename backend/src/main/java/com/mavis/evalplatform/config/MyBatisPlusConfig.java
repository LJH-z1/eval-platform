package com.mavis.evalplatform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * <p>
 * - 启用分页插件
 * - 启用 create_time / update_time 自动填充(@TableField fill = INSERT/INSERT_UPDATE)
 *
 * @author 刘家豪
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt",   LocalDateTime.class, now);
                strictInsertFill(metaObject, "createTime",  LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt",   LocalDateTime.class, now);
                strictInsertFill(metaObject, "updateTime",  LocalDateTime.class, now);
                strictInsertFill(metaObject, "created_at",  LocalDateTime.class, now);
                strictInsertFill(metaObject, "updated_at",  LocalDateTime.class, now);
            }
            @Override
            public void updateFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictUpdateFill(metaObject, "updatedAt",   LocalDateTime.class, now);
                strictUpdateFill(metaObject, "updateTime",  LocalDateTime.class, now);
                strictUpdateFill(metaObject, "updated_at",  LocalDateTime.class, now);
            }
        };
    }
}
