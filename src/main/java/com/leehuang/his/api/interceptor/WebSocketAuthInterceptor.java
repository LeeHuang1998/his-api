package com.leehuang.his.api.interceptor;

import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// ChannelInterceptor：Spring 消息通道拦截器接口，用于拦截和处理消息
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final OrderService orderService; // 注入订单服务

    /**
     * ChannelInterceptor 接口的核心方法，在消息发送之前进行拦截处理，所有入站 STOMP 帧都会先经过这里
     * @param message   被拦截的消息对象，为 Spring 的通用 Message 对象
     * @param channel   消息通道
     * @return          处理完成后的消息
     */
    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        // 将消息包装为 STOMP 头访问器，用于获取消息头中的数据
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // StompCommand：STOMP 协议定义的命令枚举
        // 1. CONNECT 阶段：判断当前是否为连接请求，如果是则验证 token，只在第一次握手时做 Token 校验；其他命令（SUBSCRIBE/SEND）跳过。
        //      StompCommand.CONNECT：STOMP 命令类型，表示客户端尝试连接
        //      accessor.getCommand()：获取当前 STOMP 帧的命令类型
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 获取 STOMP 帧头中的 Authorization 字段
            // getFirstNativeHeader：获取原始 HTTP 头，STOMP 会将 HTTP 头转换为小写，getFirstNativeHeader 保留原始大小写
            String token = accessor.getFirstNativeHeader("Authorization");
            // 只有在建立连接时才会校验 token，其他阶段时无需校验，只要在 CONNECT 阶段校验，其他阶段必定有 token。
            if (token == null || !token.startsWith("Bearer ")) {
                throw new HisException("缺少有效的 Authorization 头");
            }

            // 去除 Bearer 前缀
            token = token.substring(7).trim();

            try {
                // 2. 通过 token 获取 customerId
                Object loginId = StpCustomerUtil.getLoginIdByToken(token);
                Integer customerId = Integer.parseInt(loginId.toString());

                // 3. 将用户 ID 绑定当前 STOMP 会话，表示当前会话认证的用户
                // STOMP 会话是指客户端和服务器之间的一个持久连接，从 CONNECT 开始，到 DISCONNECT 结束，在同一个 WebSocket 连接中，所有 STOMP 帧共享同一个会话
                // accessor.setUser(Principal principal)：Principal 是 Java 安全框架的接口，代表认证主体，后续的 SUBSCRIBE/SEND 命令可以访问此用户身份
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs == null) {
                    sessionAttrs = new HashMap<>();
                }

                sessionAttrs.put("customerId", customerId);
                accessor.setSessionAttributes(sessionAttrs);

//                accessor.setUser(customerId::toString);
                log.info("WebSocket 认证成功，用户ID: {}", customerId);
            } catch (Exception e) {
                log.error("WebSocket 认证失败", e);
                throw new HisException("无效的 token: " + e.getMessage());
            }
        }

        // 4. SUBSCRIBE 阶段：判断当前是否为订阅请求，如果是则执行权限验证
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            // 获取 STOMP 帧的目标地址，即前端订阅的地址，例如 /topic/payment/20251022001
            String destination = accessor.getDestination();

            // 判断订阅地址是否为支付结果主题（payment），若为其他主题则不执行校验
            if (destination != null && destination.startsWith("/topic/payment/")) {
                // 获取订单号和 customerId（customerId 在 STOMP 会话的 CONNECT 阶段 setUser() 放入）
                String outTradeNo = destination.substring("/topic/payment/".length());

                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                Integer customerId = (Integer) sessionAttrs.get("customerId");

//                String customerIdStr = accessor.getUser().getName();

                // 验证当前用户是否有权订阅该订单
                if (!Objects.equals(orderService.searchCustomerId(outTradeNo), customerId)) {
                    log.warn("用户 {} 无权访问订单 {} 的支付状态", customerId, outTradeNo);
                    throw new HisException("无权访问该订单的支付状态");
                }
            }
        }
        // 通过校验，把消息放回管道
        return message;
    }
}