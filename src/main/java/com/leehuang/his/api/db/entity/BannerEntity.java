package com.leehuang.his.api.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_banner
 */
@Data
@TableName("tb_banner")
public class BannerEntity {

    private Integer id;

    private String name;

    private Integer goodsId;

    private String remarks;

    private String image;

    private Integer status;
}
