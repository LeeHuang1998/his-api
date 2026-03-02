package com.leehuang.his.api.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@Validated
public class MinioProperties {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String bucket;
}
