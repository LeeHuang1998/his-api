package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_goods
 */
@Data
@TableName("tb_goods")
public class GoodsEntity implements Serializable {
    private Integer id;

    private String code;

    private String title;

    private String description;

    private String checkup1;                        // 科室检查

    private String checkup2;                        // 实验室检查

    private String checkup3;                        // 医技检查

    private String checkup4;                        // 其他检查

    private String checkup;                         // 检查内容，即 checkup1-4 中 title 的具体检查内容，例如眼科检查中包含了：左右眼视力，色觉检查等等详细的检查内容

    private String image;

    private BigDecimal initialPrice;

    private BigDecimal currentPrice;

    private Integer salesVolume;

    private String type;

    private String tag;

    private Integer partId;

    private Integer ruleId;

    private Integer status;

    private String md5;

    private String updateTime;

    private String createTime;

    private static final long serialVersionUID = 1L;
}