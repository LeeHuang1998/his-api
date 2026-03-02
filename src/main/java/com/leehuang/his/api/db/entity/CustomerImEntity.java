package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * @TableName tb_customer_im
 */
@Data
@TableName("tb_customer_im")
public class CustomerImEntity implements Serializable {
    private Integer id;

    private Integer customerId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime loginTime;

    private static final long serialVersionUID = 1L;
}