package com.leehuang.his.api.front.dto.goods.vo;

import com.leehuang.his.api.mis.dto.goods.vo.CheckupItemVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsPageVO {
    private String code;

    private String title;

    private String description;

    private String[] images;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private String ruleName;

    private String type;

    private String[] tags;

    private List<CheckupItemVo> checkup_1;

    private int count_1;

    private List<CheckupItemVo> checkup_2;

    private int count_2;

    private List<CheckupItemVo> checkup_3;

    private int count_3;

    private List<CheckupItemVo> checkup_4;

    private int count_4;

}
