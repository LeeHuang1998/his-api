package com.leehuang.his.api.mis.dto.banner.vo;

import lombok.Data;

@Data
public class BannerPageVO {

    private Integer id;

    private String name;

    private String image;

    private Integer goodsId;

    private String title;

    private Boolean goodsStatus;

    private String remarks;

    private Boolean status;

}
