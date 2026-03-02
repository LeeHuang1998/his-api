package com.leehuang.his.api.front.dto.order.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class OutTradeNoRequest {

    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;
}
