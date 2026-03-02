package com.leehuang.his.api.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;

    private String gatewayUrl;

    private String notifyUrl;

    private String returnUrl;

    private String merchantPrivateKey;

    private String alipayPublicKey;

    private String signType;

    private String charset;

    private String format;

    public AlipayClient getAlipayClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                merchantPrivateKey,
                format,
                charset,
                alipayPublicKey,
                signType
        );
    }
}
