package com.leehuang.his.api.db.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_flow_regulation
 */
@Data
@TableName("tb_flow_regulation")
public class FlowRegulationEntity implements Serializable {
    private Integer id;

    private String place;

    private Integer realNum;

    private Integer maxNum;

    private Integer weight;

    private Integer priority;

    private String blueUuid;

    private Integer isDeleted;

    private static final long serialVersionUID = 1L;
}