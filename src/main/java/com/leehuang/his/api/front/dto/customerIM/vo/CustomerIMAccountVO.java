package com.leehuang.his.api.front.dto.customerIM.vo;

import lombok.Data;

@Data
public class CustomerIMAccountVO {

    private Long sdkAppId;

    private String account;

    private String userSig;
}
