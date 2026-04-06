package com.leehuang.his.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 */
@Configuration
public class ThreadPoolConfig {

    // @Bean 表示将该组件注册到 Spring 容器内，若没有显式指定 Bean 的名称，则默认使用方法名作为 Bean 的名称
    // 对于 @Component 及其派生注解（如 @Service, @Repository, @Controller），使用类名首字母小写后的字符串作为 Bean 的名字。对于方法则直接使用方法名
    @Bean("AsyncTaskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 设置核心线程数，始终保留的线程数，即使空闲也不会销毁，使用该线程执行任务的优先级最高
        taskExecutor.setCorePoolSize(20);
        // 设置最大线程数（当核心线程和任务队列都满了的情况下，就会创建非核心线程来执行任务，线程总数不会超过该值）
        taskExecutor.setMaxPoolSize(50);
        // 设置任务队列容量，当核心线程数量达到 corePoolSize 时，新任务会进入队列等待执行，队列中的任务会被线程池中的所有线程（核心线程 + 非核心线程）共同处理
        taskExecutor.setQueueCapacity(500);
        // 设置非核心线程活跃时间（秒），当线程池中的线程数超过核心线程数时，空闲的非核心线程在该时间后会被回收
        taskExecutor.setKeepAliveSeconds(60);
        // 设置线程池中线程的前缀名称
        taskExecutor.setThreadNamePrefix("his-async-task-");
        // 设置拒绝策略
        // 四种策略：AbortPolicy 直接抛出异常（默认）。CallerRunsPolicy：线程池拒绝执行，将任务交给主线程执行，保证线程一定执行。
        //         DiscardPolicy：线程池拒绝执行，直接丢弃任务，不予处理。DiscardOldestPolicy：丢弃队列中最早的任务，然后尝试提交当前任务
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池
        taskExecutor.initialize();
        return taskExecutor;
    }
}
