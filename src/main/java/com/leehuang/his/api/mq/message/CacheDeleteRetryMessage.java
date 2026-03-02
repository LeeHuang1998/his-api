package com.leehuang.his.api.mq.message;

import lombok.Data;

@Data
public class CacheDeleteRetryMessage {

    private String cacheKey;

    private int retryCount;
}
