package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_dept
 */
@Data
@TableName("tb_dept")
public class DeptEntity implements Serializable {
    private Integer id;

    private String deptName;

    private String tel;

    private String email;

    private String desc;

    private static final long serialVersionUID = 1L;
}