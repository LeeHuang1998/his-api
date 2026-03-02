package com.leehuang.his.api.config.xss;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @WebFilter：过滤器组件，拦截所有请求
 * 请求执行流程：客户端 → XssFilter → Spring MVC DispatcherServlet → Controller
 */
@WebFilter(urlPatterns = "/*")
public class XssFilter implements Filter {

    /**
     * 过滤器初始化方法，在容器启动时调用一次，无需特殊初始化时为空
     * @param filterConfig          包含过滤器初始化参数和ServletContext
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    /**
     * 过滤器核心方法，在每次请求时调用，在请求到达 Servlet 之前和 响应返回客户端之前 都会经过此方法
     * @param servletRequest            原始请求对象
     * @param servletResponse           原始响应对象
     * @param filterChain               过滤器链，用于将请求传递给下一个过滤器或目标资源
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1. 将原始请求通用 ServletRequest 转换为 HttpServletRequest，HttpServletRequest 包含 HTTP 特有的方法（如获取请求头、方法类型等）
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // 2. 创建自定义的 XSS 防护的请求包装器包装请求，保持原始请求功能的同时添加 XSS 过滤能力
        XssHttpServletRequestWrapper wrapper = new XssHttpServletRequestWrapper(request);
        // 3. 将包装后的请求传递给过滤器链的下一个组件，后续过滤器和 Servlet 将接收到已过滤的请求
        filterChain.doFilter(wrapper, servletResponse);
    }

    /**
     * 过滤器销毁时调用，用于清理资源
     */
    @Override
    public void destroy() {

    }
}
