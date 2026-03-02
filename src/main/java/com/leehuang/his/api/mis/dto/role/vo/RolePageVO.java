package com.leehuang.his.api.mis.dto.role.vo;

import lombok.Data;

@Data
public class RolePageVO {

    private Integer id;

    private String roleName;

    private Integer permissions;

    private Integer users;

    private String desc;

    private Integer systemic;
}
