package com.leehuang.his.api.mis.controller;

import com.leehuang.his.api.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RequestMapping("/test")
@RestController
public class TestController {


    // 设置所有请求都需要经过该过滤器，使用该注解后需要在主启动类上添加注解 @ServletComponentScan
    // 因为 Spring Boot 默认不会扫描这些原生的 Servlet 组件（包括 @WebFilter、@WebListener、@WebServlet 等），需要通过 @ServletComponentScan 注解进行扫描。
    @PostMapping("/testFilter")
    public HashMap<String, String> testFilter(String str){
        System.out.println("传入的字符串：" + str);
        return new HashMap<>() {
            {
                put("msg", "Hello, World!");
            }
        };
    }

    @GetMapping("/testKeyStore")
    public R testKeyStore(){
        return R.OK("执行成功，Hello World");
    }
}
