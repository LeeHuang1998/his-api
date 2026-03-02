package com.leehuang.his.api.front.dto.goods;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 从数据库中查出，用于转换 GoodsPageVO
 */
@Data
public class FrontGoodsDataDTO {

    private String code;

    private String title;

    private String description;

    private String images;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private String ruleName;

    private String type;

    private String tags;

    private String checkup1;

    private int count_1;

    private String checkup2;

    private int count_2;

    private String checkup3;

    private int count_3;

    private String checkup4;

    private int count_4;

}
