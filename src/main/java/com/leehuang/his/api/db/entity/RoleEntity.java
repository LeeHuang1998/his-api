package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_role
 */
@Data
@TableName("tb_role")
public class RoleEntity implements Serializable {
    private Integer id;

    private String roleName;

    private String permissions;

    private String desc;

    private String defaultPermissions;

    private Integer systemic;

    private static final long serialVersionUID = 1L;
}