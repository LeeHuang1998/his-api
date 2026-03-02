package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_rule
 */
@Data
@TableName("tb_rule")
public class RuleEntity implements Serializable {
    private Integer id;

    private String name;

    private String rule;

    private String remark;

    private static final long serialVersionUID = 1L;
}