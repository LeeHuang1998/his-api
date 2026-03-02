package com.leehuang.his.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    // @Bean 表示将该组件注册到 Spring 容器内，若没有显式指定 Bean 的名称，则默认使用方法名作为 Bean 的名称
    // 对于 @Component 及其派生注解（如 @Service, @Repository, @Controller），使用类名首字母小写后的字符串作为 Bean 的名字。对于方法则直接使用方法名
    @Bean("AsyncTaskExecutor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        taskExecutor.setCorePoolSize(8);
        // 设置最大线程数
        taskExecutor.setMaxPoolSize(16);
        // 设置任务队列容量，当所有线程都没有空闲时，任务会被放到任务队列中等待，直到有线程空闲来处理任务
        taskExecutor.setQueueCapacity(1000);
        // 设置线程活跃时间（秒）
        taskExecutor.setKeepAliveSeconds(60);
        // 设置线程池的前缀名称
        taskExecutor.setThreadNamePrefix("task-");
        // 设置拒绝策略
        // 四种策略：AbortPolicy 直接抛出异常，默认拒绝方案。CallerRunsPolicy：线程池拒绝执行，将任务交给主线程执行，保证线程一定执行。
        //         DiscardPolicy：线程池拒绝执行，直接丢弃任务，不予处理。DiscardOldestPolicy：将最先进入任务队列中的任务丢弃
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化线程池
        taskExecutor.initialize();
        return taskExecutor;
    }
}
