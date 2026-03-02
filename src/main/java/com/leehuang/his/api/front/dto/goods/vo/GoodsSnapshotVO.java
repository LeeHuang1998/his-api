package com.leehuang.his.api.front.dto.goods.vo;

import com.leehuang.his.api.mis.dto.goods.vo.CheckupItemVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsSnapshotVO {

    private Integer goodsId;

    private String code;

    private String title;

    private String description;

    private List<CheckupItemVo> checkup1;

    private List<CheckupItemVo> checkup2;

    private List<CheckupItemVo> checkup3;

    private List<CheckupItemVo> checkup4;

    private String image;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private String type;

    private String[] tag;

    private String ruleName;


}
