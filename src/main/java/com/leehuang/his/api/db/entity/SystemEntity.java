package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_system
 */
@Data
@TableName("tb_system")
public class SystemEntity implements Serializable {
    private Integer id;

    private String item;

    private String value;

    private String remark;

    private static final long serialVersionUID = 1L;
}