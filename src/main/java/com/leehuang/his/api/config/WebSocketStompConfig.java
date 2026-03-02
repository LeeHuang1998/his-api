package com.leehuang.his.api.config;

import com.leehuang.his.api.interceptor.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// 启用 WebSocket 消息代理，提供 STOMP 协议支持，没有这个注解就不会启动 WebSocket 消息路由
@EnableWebSocketMessageBroker
@Configuration
@RequiredArgsConstructor
public class WebSocketStompConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor; //  注入拦截器

    /**
     * 配置客户端入站通道（从客户端到服务器的消息流），将鉴权拦截器插到消息进入的管道里；CONNECT / SUBSCRIBE 帧（阶段）都会先被它拦一遍
     * @param registration Spring 的通道注册对象，用于配置拦截器
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 注册认证拦截器，所有入站消息都会经过此拦截器处理
        registration.interceptors(authInterceptor);
    }

    /**
     * 注册 STOMP 端点，客户端连接的入口，定义前端如何连接到 WebSocket 服务
     * @param registry WebSocket 端点注册器，用于添加和配置端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //  WebSocket 握手端点，前端连接的端点（与前端 new SockJS('/ws') 匹配）
        registry.addEndpoint("/ws")                         // 端点路径
                .setAllowedOriginPatterns("*")                     // 允许任意域名跨域连接
//                .setAllowedOriginPatterns("http://localhost:5173", "https://your-frontend.com")
                .withSockJS();                                     // 启用 SocketJS 降级，不支持 WebSocket 时自动用轮询/XHR 兼容
    }

    /**
     * 配置消息代理，定义消息的路由规则和处理方式
     * @param registry 消息代理注册器，用于配置代理行为
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单内存消息代理，配置消息代理前缀，订阅消息时以 /topic 开头，即前端订阅的地址，负责把消息转发给所有订阅者
        // 消息代理前缀（前端 subscribe 的 /topic/... 会路由到这里）/topic/ 前缀表示广播消息，所有订阅该主题的客户端都会收到消息
        registry.enableSimpleBroker("/topic");
        // 前端发送消息时的前缀，前端发送到 /app/xxx 的消息会路由到对应的消息处理方法
        registry.setApplicationDestinationPrefixes("/app");
    }
}