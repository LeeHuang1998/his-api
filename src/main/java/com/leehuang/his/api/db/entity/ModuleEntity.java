package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_module
 */
@Data
@TableName("tb_module")
public class ModuleEntity implements Serializable {
    private Integer id;

    private String moduleCode;

    private String moduleName;

    private static final long serialVersionUID = 1L;
}