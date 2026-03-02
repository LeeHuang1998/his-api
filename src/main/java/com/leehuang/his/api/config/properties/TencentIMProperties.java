package com.leehuang.his.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "tencent.im")
@Data
public class TencentIMProperties {

    private Long sdkAppId;

    private String secretKey;

    private String managerId;

    private String customerServiceId;

    private String baseUrl = "https://console.tim.qq.com/";
}
