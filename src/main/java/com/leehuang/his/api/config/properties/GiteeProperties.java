package com.leehuang.his.api.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "oauth.gitee")
@Data
public class GiteeProperties {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String accessTokenUrl;

    private String userInfoUrl;
}
