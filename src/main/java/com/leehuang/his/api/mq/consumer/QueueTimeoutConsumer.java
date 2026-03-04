package com.leehuang.his.api.mq.consumer;

import com.leehuang.his.api.common.constants.redis.FlowRegulationConstants;
import com.leehuang.his.api.mq.message.FlowTimeoutMessage;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;


@Component
@RequiredArgsConstructor
@Slf4j
public class QueueTimeoutConsumer {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 监听死信队列（排队超时）
     * @param message           发送的消息
     * @param channel            消息通道
     * @param mqMessage          消息体
     * @throws IOException
     */
    @RabbitListener(queues = "flow.timeout.queue")
    public void consume(
            FlowTimeoutMessage message,
            Channel channel,
            Message mqMessage
    ) throws IOException {

        // 幂等 Key
        String rankKey = FlowRegulationConstants.QUEUE_MAP_PREFIX + message.getUuid() + ":" + message.getPlaceId();

        try {
            String lua = "if redis.call('GET', KEYS[1]) == 'PENDING' then " +               // 只有当前体检人的排队状态为 PENDING 时，才执行下面的操作
                    "  redis.call('ZINCRBY', KEYS[2], -1, ARGV[1]); " +                     // 排队人数 -1
                    "  redis.call('ZREM', KEYS[3], ARGV[2]); " +                            // 从排队人员姓名队列中移除指定的排队人姓名
                    "  redis.call('SET', KEYS[1], 'DONE', 'EX', 600); " +                   // 排队消息标记为 DONE，防止重复回收，若不存在会自动新建该 key 且 value 为 DONE
                    "  return 1; " +                                                        // 成功返回 1，否则返回 0
                    "else return 0; end";

            Long result = stringRedisTemplate.execute(
                    new DefaultRedisScript<>(lua, Long.class),
                    Arrays.asList(
                            rankKey,
                            FlowRegulationConstants.FLOW_PLACE_RANK,
                            FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + message.getPlaceId()
                    ),
                    message.getPlaceId().toString(),
                    message.getUuid() + ":" + message.getName()
            );

            // 根据 result 决定 ACK/NACK
            if (result == null) {
                //  Redis 执行异常（如网络、脚本错误）
                log.warn("Redis 脚本执行返回 null，消息将重试。uuid={}, placeId={}, place={}",
                        message.getUuid(), message.getPlaceId(), message.getPlace());
                // 异常重试，消息重新入队，3 个参数：
                //      deliveryTag：消息的唯一编号
                //      b：multiple，false 表示只拒绝当前这一条消息；true 表示拒绝所有小于等于该 tag 的未确认消息
                //      b1：requeue，true 表示将消息重新放回队列尾部，等待再次投递；false 表示直接丢弃或发送到死信队列
                channel.basicNack(mqMessage.getMessageProperties().getDeliveryTag(), false, true);

            } else if (result == 1L) {
                // 成功执行 lua
                log.info("排队超时回收成功：uuid={}, placeId={}, place={}", message.getUuid(), message.getPlaceId(), message.getPlace());
                channel.basicAck(mqMessage.getMessageProperties().getDeliveryTag(), false);

            } else if (result == 0L) {
                // 状态不是 PENDING（已被医生提前处理或已回收），幂等场景
                log.info("排队已处理，跳过超时回收（幂等）：uuid={}, placeId={},place={}",
                        message.getUuid(), message.getPlaceId(), message.getPlace());
                channel.basicAck(mqMessage.getMessageProperties().getDeliveryTag(), false);

            } else {
                // 防御性处理
                log.error("Lua 脚本返回未知值: {}, uuid={}, placeId={},place={}",
                        result, message.getUuid(), message.getPlaceId(), message.getPlace());
                channel.basicNack(mqMessage.getMessageProperties().getDeliveryTag(), false, true);
            }
        } catch (Exception e) {
            // 异常重试，消息重新入队
            channel.basicNack(mqMessage.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
