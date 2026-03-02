package com.leehuang.his.api.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class FlowRegulationRabbitConfig {

    /** 业务交换机（Direct） */
    @Bean
    public DirectExchange flowExchange() {
        // durable=true：MQ 重启不丢，autoDelete=false：不自动删除
        return new DirectExchange("flow.exchange", true, false);
    }

    /** 死信交换机 */
    @Bean
    public DirectExchange flowDlxExchange() {
        return new DirectExchange("flow.dlx.exchange", true, false);
    }

    /**
     * 延迟队列（不设置队列级 TTL，使用消息级 TTL），支持不同业务使用不同延迟时间
     */
    @Bean
    public Queue flowDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 指定死信交换机
        args.put("x-dead-letter-exchange", "flow.dlx.exchange");
        // 指定死信 routingKey
        args.put("x-dead-letter-routing-key", "flow.timeout");
        return new Queue("flow.delay.queue", true, false, false, args);
    }

    /**
     * 死信队列（真正被消费的队列）
     */
    @Bean
    public Queue flowTimeoutQueue() {
        return new Queue("flow.timeout.queue", true);
    }

    /**
     * 延迟队列绑定到业务交换机
     */
    @Bean
    public Binding flowDelayBinding() {
        return BindingBuilder
                .bind(flowDelayQueue())
                .to(flowExchange())
                .with("flow.delay");
    }

    /**
     * 死信队列绑定到死信交换机
     */
    @Bean
    public Binding flowTimeoutBinding() {
        return BindingBuilder
                .bind(flowTimeoutQueue())
                .to(flowDlxExchange())
                .with("flow.timeout");
    }

    /**
     * JSON 消息转换器，让 MQ 自动序列化 / 反序列化对象，全局复用
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
