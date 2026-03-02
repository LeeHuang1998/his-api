package com.leehuang.his.api.front.dto.pay.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class PayRequest {

    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;   // 商户订单号（唯一）

    @NotBlank(message = "商品标题不能为空")
    private String subject;      // 商品标题

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0.01元")
    private String totalAmount;  // 付款金额

    @NotBlank(message = "支付方式不能为空")
    private String paymentType;  // 支付方式

    @NotNull(message = "收货地址ID不能为空")
    @Min(value = 1, message = "收货地址ID无效")
    private Integer addressId;   // 地址id

    private Boolean isRefresh;  // 是否为刷新二维码操作
}
