package com.leehuang.his.api.common.constants.redis;

public class FlowRegulationConstants {

    public static final String FLOW_INIT_STATUS = "flow:init:status";

    /** 科室调流数据 **/
    public static final String FLOW_REGULATION = "flow:regulation";

    /** 已完成科室 **/
    public static final String CHECKUP_FINISHED_PREFIX = "checkup:finished:place:";

    /** 全局调流 ZSET 的 key，member：科室，score：排队人数 **/
    public static final String FLOW_PLACE_RANK = "flow:place:rank";

    /** 是否为自动调流模式 **/
    public static final String AUTO_FLOW_REGULATION = "auto_flow_regulation";

    /** 排队队列的 key 前缀，用于标记当前体检排队的状态 **/
    public static final String QUEUE_MAP_PREFIX = "queue:map:";

    /** 排队队列的 key 前缀，用于标记当前体检排队人员的姓名 **/
    public static final String FLOW_REGULATION_QUEUE_PREFIX = "flow:regulation:place_queue:";
}
