package com.leehuang.his.api.mis.dto.flowRegulation.vo;

import lombok.Data;

@Data
public class RealTimeQueueDataVO {

    /** 科室id **/
    private Integer id;

    /** 科室名 **/
    private String place;

    /** 当前排队人数 **/
    private Integer realNum;

    /** 排队人数上限 **/
    private Integer maxNum;
}
