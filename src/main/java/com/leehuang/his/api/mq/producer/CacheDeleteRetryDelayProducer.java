package com.leehuang.his.api.mq.producer;

import com.leehuang.his.api.mq.message.CacheDeleteRetryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheDeleteRetryDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 缓存删除延迟消息
     * @param message           发送的消息
     * @param delayMillis       消息过期时间
     */
    public void sendCacheDeleteRetryDelayMessage(CacheDeleteRetryMessage message, long delayMillis) {

        // 发送消息到业务交换机，进入延迟队列
        rabbitTemplate.convertAndSend(
                "retry.exchange",                // 交换机
                "cache.delete.retry.delay",               // routingKey
                message,                                  // 消息体（自动 JSON 序列化）
                msg -> {
                    // 设置消息级 TTL
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    return msg;
                }
        );

        log.debug("缓存延迟消息发送，message：{}, 时间：{}", message, LocalDateTime.now());
    }
}
