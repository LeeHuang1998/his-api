package com.leehuang.his.api.common.constants.mq;

public class QueueConstants {

    /** 业务交换机（发送延迟消息） */
    public static final String QUEUE_EXCHANGE = "queue.exchange";

    /** 延迟队列（30 分钟后过期） */
    public static final String QUEUE_DELAY = "queue.delay";

    /** 延迟队列 routing key */
    public static final String QUEUE_DELAY_KEY = "queue.delay.key";

    /** 死信交换机（真正消费） */
    public static final String QUEUE_DLX_EXCHANGE = "queue.dlx.exchange";

    /** 死信队列 */
    public static final String QUEUE_DLX = "queue.dlx";

    /** 死信 routing key */
    public static final String QUEUE_DLX_KEY = "queue.dlx.key";

}
