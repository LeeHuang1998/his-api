package com.leehuang.his.api.mis.dto.goods.vo;

import lombok.Data;

@Data
public class GoodsMisPageVO {

    private int id;

    private String title;

    private String code;

    private String initialPrice;

    private String currentPrice;

    private String salesVolume;

    private String ruleName;

    private String type;

    private Boolean status;

    private Integer partId;

    private Boolean hasCheckup;
}
