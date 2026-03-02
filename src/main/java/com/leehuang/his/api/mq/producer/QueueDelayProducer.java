package com.leehuang.his.api.mq.producer;

import com.leehuang.his.api.mq.message.FlowTimeoutMessage;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 消息发送者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QueueDelayProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送延迟扣减排队人数的消息
     * @param message           发送的消息
     * @param delayMillis       消息过期时间
     */
    public void sendQueueTimeoutMessage(FlowTimeoutMessage message, long delayMillis) {

        // 发送消息到业务交换机，进入延迟队列
        rabbitTemplate.convertAndSend(
                "flow.exchange",                // 交换机
                "flow.delay",                            // routingKey
                message,                                 // 消息体（自动 JSON 序列化）
                msg -> {
                    // 设置消息级 TTL
                    msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    return msg;
                }
        );

        log.debug("发送 {} 分钟的延迟消息到 flow.delay 中，message：{}, 时间：{}",
                TimeUnit.MICROSECONDS.toMinutes(delayMillis),
                message,
                LocalDateTime.now()
        );
    }
}