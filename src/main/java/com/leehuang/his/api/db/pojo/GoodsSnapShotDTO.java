package com.leehuang.his.api.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;

@Data
public class GoodsSnapShotDTO {

    private String code;

    private String title;

    private String description;

    private String checkup1;

    private String checkup2;

    private String checkup3;

    private String checkup4;

    private String image;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private String type;

    private String tag;

    private String ruleName;

    private String rule;

    private String checkup;

    private String md5;
}
