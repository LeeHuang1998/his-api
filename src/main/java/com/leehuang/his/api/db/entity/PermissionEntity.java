package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_permission
 */
@Data
@TableName("tb_permission")
public class PermissionEntity implements Serializable {
    private Integer id;

    private String permissionName;

    private Integer moduleId;

    private Integer actionId;

    private static final long serialVersionUID = 1L;
}