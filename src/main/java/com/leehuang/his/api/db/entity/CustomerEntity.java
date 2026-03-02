package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_customer
 */
@Data
@TableName("tb_customer")
public class CustomerEntity implements Serializable {
    private Integer id;

    private String username;

    private String password;

    private String name;

    private String sex;

    private String tel;

    private String email;

    private String photo;

    private String thirdParty;

    private LocalDate createTime;

    private static final long serialVersionUID = 1L;
}