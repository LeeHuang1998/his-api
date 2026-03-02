package com.leehuang.his.api.mis.dto.user.vo;

import lombok.Data;

@Data
public class UserDetailVO {
    private Integer id;

    private String username;

    private String name;

    private String sex;

    private String tel;

    private String email;

    private String hiredate;

    private Integer[] role;

    private Integer deptId;
}
