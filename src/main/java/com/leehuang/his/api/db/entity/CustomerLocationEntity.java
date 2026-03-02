package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_customer_location
 */
@Data
@TableName("tb_customer_location")
public class CustomerLocationEntity implements Serializable {
    private Integer id;

    private Integer customerId;

    private String blueUuid;

    private Integer placeId;

    private String createTime;

    private static final long serialVersionUID = 1L;
}