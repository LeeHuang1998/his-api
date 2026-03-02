package com.leehuang.his.api.mis.dto.flowRegulation.vo;

import lombok.Data;

@Data
public class FlowRegulationPageVO {

    private Integer id;

    private String place;

    private Integer maxNum;

    /**
     * 权重
     */
    private Integer weight;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 蓝牙 uuid
     */
    private String blueUuid;

    private Integer isDeleted;
}
