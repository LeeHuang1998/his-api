package com.leehuang.his.api.mq.consumer;

import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mq.message.CacheDeleteRetryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheDeleteRetryConsumer {

    private static final int MAX_RETRY = 3;

    private final StringRedisTemplate redisTemplate;

    private final RabbitTemplate rabbitTemplate;

    /**
     * 缓存删除重试
     * @param message
     */
    @RabbitListener(queues = "cache.delete.retry.timeout.queue")
    public void handleCacheDeleteRetry(CacheDeleteRetryMessage message) {
        String cacheKey = message.getCacheKey();
        try {
            // 1. 缓存中删除数据
            redisTemplate.delete(cacheKey);
            log.info("缓存删除成功，cacheKey: {}", cacheKey);
        } catch (Exception e) {
            // 2. 缓存删除失败，加入重试队列
            // 2.1 获取重试次数
            int retryCount = message.getRetryCount();
            if (retryCount < MAX_RETRY) {
                // 2.3 重试次数 + 1
                message.setRetryCount(retryCount + 1);
                // 2.4 发送到延迟队列
                rabbitTemplate.convertAndSend(
                        "retry.exchange",                     // 交换机
                        "cache.delete.retry.delay",                    // 路由键（与绑定一致）
                        message,
                        msg -> {
                            msg.getMessageProperties().setExpiration(String.valueOf(10000)); // 消息级TTL
                            return msg;
                        });
                log.warn("重新尝试删除 cacheKey: {}, 重试次数: {}", cacheKey, message.getRetryCount());
            } else {
                log.error("缓存删除失败，重试已达最大次数，cacheKey: {}", cacheKey);
                // 告警机制（用抛出异常替代）
                throw new HisException("缓存【" + message.getCacheKey() + "】删除失败，重试已达最大次数，请联系管理员");
            }
        }
    }
}
