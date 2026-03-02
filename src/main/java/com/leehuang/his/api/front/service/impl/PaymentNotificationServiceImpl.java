package com.leehuang.his.api.front.service.impl;

import com.leehuang.his.api.front.service.PaymentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("paymentNotificationService")
@RequiredArgsConstructor
public class PaymentNotificationServiceImpl implements PaymentNotificationService {

    // Spring STOMP 客户端
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送支付结果到指定订单
     * @param outTradeNo 订单号
     * @param status 状态：success/failure
     */
    public void sendPaymentResult(String outTradeNo, String status) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("outTradeNo", outTradeNo);
            message.put("status", status);

            // 消息目的地，推送到 /topic/payment/{outTradeNo}，即前端订阅的地址
            String destination = "/topic/payment/" + outTradeNo;
            // 将消息转换并发送到指定目的地，推送给所有订阅了该主题且通过拦截器鉴权的会话
            messagingTemplate.convertAndSend(destination, message);
            
            log.info("支付结果已推送: 订单号={}, 状态={}", outTradeNo, status);
        } catch (Exception e) {
            log.error("WebSocket 推送失败，订单号: {}", outTradeNo, e);
        }
    }
}