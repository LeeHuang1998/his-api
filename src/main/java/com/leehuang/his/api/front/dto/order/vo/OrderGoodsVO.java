package com.leehuang.his.api.front.dto.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderGoodsVO {

    private String title;

    private String description;

    private String images;

    private BigDecimal currentPrice;

    private String ruleName;
}
