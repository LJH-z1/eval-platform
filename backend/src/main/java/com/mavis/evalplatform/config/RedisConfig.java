package com.mavis.evalplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * <p>
 * key 字符串序列化,value JSON 序列化(供其他模块做缓存使用)
 *
 * @author 刘家豪
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf, ObjectMapper mapper) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        StringRedisSerializer str = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(mapper);
        t.setKeySerializer(str);
        t.setHashKeySerializer(str);
        t.setValueSerializer(json);
        t.setHashValueSerializer(json);
        t.afterPropertiesSet();
        return t;
    }
}
