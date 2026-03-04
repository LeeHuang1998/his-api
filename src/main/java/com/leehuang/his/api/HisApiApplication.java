package com.leehuang.his.api;

import com.leehuang.his.api.async.InitializeWorkAsync;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
*  @EnableAsync：启用异步方法执行，激活 Spring 的异步处理能力，让被 @Async 标注的方法或类的所有方法能够被异步执行
*  @ServletComponentScan：启用 Servlet 组件自动注册：该注解用于扫描其所在的包及其子包，并注册使用原生 Servlet API 注解（@WebServlet、@WebFilter和 @WebListener）标记的组件
 *                        由于自定义了 XSS 过滤器（XSSFilter），需要添加该注解
 * @EnableCaching：启用 SpringCache 缓存功能
 */
@EnableAsync
@ServletComponentScan
@EnableCaching
@MapperScan("com.leehuang.his.api.db.dao")
@ConfigurationPropertiesScan("com.leehuang.his.api.config.properties")
@SpringBootApplication
public class HisApiApplication {

    @Resource
    private InitializeWorkAsync initializeWorkAsync;

    public static void main(String[] args) {
        SpringApplication.run(HisApiApplication.class, args);
    }

    /**
     * 项目启动时执行
     */
    @PostConstruct
    public void init() {
        initializeWorkAsync.initCacheAppointmentData();
    }
}