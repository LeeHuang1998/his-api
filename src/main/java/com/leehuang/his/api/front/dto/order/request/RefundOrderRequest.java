package com.leehuang.his.api.front.dto.order.request;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class RefundOrderRequest {

    @NotBlank(message = "订单号不能为空")
    private String outTradeNo;

    @NotNull(message = "商品id不能为空")
    private Integer goodsId;

    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量最小为 1")
    @Max(value = 10, message = "商品数量最大为 10")
    private Integer goodsCount;

    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    private BigDecimal refundAmount;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因长度不能超过 200 字符")
    private String refundReason;
}
