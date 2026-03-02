package com.leehuang.his.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * RedisTemplate 序列化配置
 */
@Configuration
public class RedisTemplateConfig {

    /**
     * mis 端使用 1 号数据库
     * @param factory
     * @return
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());                             // redis key 序列化
        // template.setValueSerializer(new StringRedisSerializer());                 // 原来的 redis value 序列化
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());              // redis value 序列化
        template.setHashKeySerializer(new StringRedisSerializer());                         // redis hash key 序列化
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());          // redis hash value 序列化
        template.setConnectionFactory(factory);                                             // redis 连接工厂
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 统一 TTL
                .entryTtl(Duration.ofMinutes(30))
                // 序列化 key、value
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
