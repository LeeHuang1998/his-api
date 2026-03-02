package com.leehuang.his.api.front.dto.address.vo;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class AddressVO {

    private Integer id;

    private String name;

    private String tel;

    private String province;

    private String city;

    private String district;

    private String[] regionCode;

    private String detail;

    private Boolean isDefault;

    /** 数据库：JSON 字符串，不给前端看 */
    @JsonIgnore
    private String regionCodeJson;

    /* 以下两个方法仅供 MyBatis 存/取时调用 */
    public void setRegionCodeJson(String json) {
        this.regionCodeJson = json;
        this.regionCode = JSON.parseObject(json, String[].class);
    }

    public String getRegionCodeJson() {
        if (regionCodeJson == null && regionCode != null) {
            regionCodeJson = JSON.toJSONString(regionCode);
        }
        return regionCodeJson;
    }
}
