package com.leehuang.his.api.db.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_address
 */
@Data
@TableName("tb_address")
public class AddressEntity {

    private Integer id;

    private Integer customerId;

    private String name;

    private String tel;

    private String province;

    private String city;

    private String district;

    private String regionCode;

    private String detail;

    private Boolean isDefault;
}
