package com.leehuang.his.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "aliyun")
public class AliyunProperties {

    private String accessKeyId;
    private String accessKeySecret;

    private SmsProperties sms;

    @Data
    public static class SmsProperties {
        private String endpoint;
        private String signName;
        private String templateCode;
        private Long expireMinutes;
    }
}
