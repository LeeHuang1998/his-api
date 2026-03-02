package com.leehuang.his.api.config;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import com.leehuang.his.api.config.properties.AliyunProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AliyunSmsConfig {

    private final AliyunProperties aliyunProperties;

    @Bean
    public Client dypnsClient() throws Exception {
        Config config = new Config()
                .setAccessKeyId(aliyunProperties.getAccessKeyId())
                .setAccessKeySecret(aliyunProperties.getAccessKeySecret());
        config.endpoint = aliyunProperties.getSms().getEndpoint();
        return new Client(config);
    }
}
