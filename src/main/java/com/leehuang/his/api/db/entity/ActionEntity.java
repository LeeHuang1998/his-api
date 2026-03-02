package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_action
 */
@Data
@TableName("tb_action")
public class ActionEntity implements Serializable {
    private Integer id;

    private String actionCode;

    private String actionName;

    private static final long serialVersionUID = 1L;
}