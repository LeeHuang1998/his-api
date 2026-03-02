package com.leehuang.his.api.mis.dto.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsDetailVO {

    private Integer id;

    private String code;

    private String title;

    private String description;

    private List<CheckupItemVo> checkup_1;

    private List<CheckupItemVo> checkup_2;

    private List<CheckupItemVo> checkup_3;

    private List<CheckupItemVo> checkup_4;

    private String[] images;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private String type;

    private String[] tags;

    private Integer partId;

    private Integer ruleId;
}
