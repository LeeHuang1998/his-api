package com.leehuang.his.api.config;

import cn.hutool.core.lang.Snowflake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SnowflakeConfig {

    // 工作节点 ID，默认值为 1
    @Value("${app.snowflake.worker-id:1}")
    private long workerId;

    // 数据中心ID，默认值为 1
    @Value("${app.snowflake.datacenter-id:1}")
    private long datacenterId;

    @Bean
    public Snowflake snowflake() {
        // 校验范围
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId 必须在 0-31 之间");
        }
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException("datacenterId 必须在 0-31 之间");
        }

        log.info("Snowflake 初始化 -> datacenterId={}, workerId={}", datacenterId, workerId);
        return new Snowflake(workerId, datacenterId);
    }
}