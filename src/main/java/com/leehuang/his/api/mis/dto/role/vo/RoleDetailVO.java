package com.leehuang.his.api.mis.dto.role.vo;

import lombok.Data;

@Data
public class RoleDetailVO {

    private Integer id;

    private String roleName;

    private String permissions;

    private String desc;

    private String defaultPermissions;

    private Integer systemic;
}
