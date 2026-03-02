package com.leehuang.his.api.front.dto.index.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GoodsItemVO {

    private Integer id;

    private String code;

    private String title;

    private String description;

    private String image;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private Integer salesVolume;

    private Integer partId;
}
