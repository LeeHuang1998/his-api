package com.leehuang.his.api.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CacheRetryRabbitConfig {

    /** 重试交换机（Direct） */
    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange("retry.exchange", true, false);
    }

    /** 死信交换机 */
    @Bean
    public DirectExchange retryDlxExchange() {
        return new DirectExchange("retry.dlx.exchange", true, false);
    }

    /**
     * 缓存删除重试的延迟队列（设置死信交换机和路由键）
     */
    @Bean
    public Queue cacheDeleteRetryDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "retry.dlx.exchange");
        args.put("x-dead-letter-routing-key", "cache.delete.retry.timeout");
        // 可以不设置队列级TTL，使用消息级TTL
        return new Queue("cache.delete.retry.delay.queue", true, false, false, args);
    }

    /**
     * 缓存删除重试的死信队列（真正消费的队列）
     */
    @Bean
    public Queue cacheDeleteRetryTimeoutQueue() {
        return new Queue("cache.delete.retry.timeout.queue", true);
    }

    /**
     * 将延迟队列绑定到业务交换机
     */
    @Bean
    public Binding deleteRetryDelayBinding() {
        return BindingBuilder
                .bind(cacheDeleteRetryDelayQueue())
                .to(retryExchange())
                .with("cache.delete.retry.delay");
    }

    /**
     * 将死信队列绑定到死信交换机
     */
    @Bean
    public Binding deleteRetryTimeoutBinding() {
        return BindingBuilder
                .bind(cacheDeleteRetryTimeoutQueue())
                .to(retryDlxExchange())
                .with("cache.delete.retry.timeout");
    }

    // 消息转换器已经在 FlowRegulationRabbitConfig 中定义了 messageConverter Bean，全局复用，其他配置类无需重复定义，Spring 会自动使用该转换器
}
