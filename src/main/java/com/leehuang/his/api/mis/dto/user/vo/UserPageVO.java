package com.leehuang.his.api.mis.dto.user.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

// 接收分页查询用户数据
@Data
public class UserPageVO implements Serializable {

    private int id;

    private String name;

    private String sex;

    private String tel;

    private String email;

    private String dept;

    private String hiredate;

    private Integer root;

    private Integer status;

    private String roles;

    @Serial
    private static final long serialVersionUID = 1L;

}
