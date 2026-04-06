package com.leehuang.his.api.common.enums;

import lombok.Getter;

@Getter
public enum ReportStatusEnum {

    /**
     * 报告状态码
     */
    NOT_GENERATED(1, "未生成"),
    GENERATING(2,"生成中"),
    GENERATED(3, "已生成"),
    SENT(4, "已邮寄");


    private final Integer code;
    private final String msg;

    ReportStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
