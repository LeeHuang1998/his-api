package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_user
 */
@Data
@TableName("tb_user")
public class UserEntity implements Serializable {
    private Integer id;

    private String username;

    private String password;

    private String openId;

    private String photo;

    private String name;

    private String sex;

    private String tel;

    private String email;

    private String hiredate;

    private String role;

    private Integer root;

    private Integer deptId;

    private Integer status;

    private String createTime;

    private static final long serialVersionUID = 1L;
}