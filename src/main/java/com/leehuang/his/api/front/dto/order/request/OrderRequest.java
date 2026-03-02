package com.leehuang.his.api.front.dto.order.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class OrderRequest {

    @NotNull(message = "商品 id 不能为空")
    @Min(value = 1,message = "商品 id 必须大于 0")
    private Integer id;

    @Min(value = 1,message = "goodsNum 必须大于 0")
    @Max(value = 10, message = "goodsNum 不能超过 10")
    private Integer goodsNum;
}
