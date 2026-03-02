package com.leehuang.his.api.mq.message;

import lombok.Data;

/**
 * 排队超时消息体
 */
@Data
public class FlowTimeoutMessage {

    /** 体检人姓名 **/
    private String name;

    /** 体检科室 id **/
    private Integer placeId;

    /** 科室名称 */
    private String place;

    /** 体检单唯一 uuid */
    private String uuid;
}