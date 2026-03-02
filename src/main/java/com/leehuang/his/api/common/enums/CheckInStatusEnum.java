package com.leehuang.his.api.common.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum CheckInStatusEnum {

    /**
     * 订单状态码
     */
    CHECK_FAILED(-1, "签到失败"),
    NULL_APPOINTMENT(0, "没有预约"),
    NOT_CHECK_IN(1, "未签到"),
    CHECK_IN(2, "已签到"),
    CHECK_FINISHED(3, "已结束"),
    CHECK_CLOSED(4, "已关闭");
    

    private final Integer code;
    private final String msg;

    CheckInStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // 使用 Map 缓存，O(1) 时间复杂度查找
    private static final Map<Integer, CheckInStatusEnum> CODE_MAP = new HashMap<>();

    // 类首次被加载时，自动将所有枚举实例存入 CODE_MAP 缓存
    static {
        for (CheckInStatusEnum status : values()) {
            CODE_MAP.put(status.getCode(), status);
        }
    }

    /**
     * 根据 code 获取枚举
     * @param code  状态码
     * @return      对应的枚举对象，不存在时返回 null
     */
    public static CheckInStatusEnum fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据 code 直接获取 msg（推荐）
     * @param code 状态码
     * @return 对应的描述，不存在时返回默认值
     */
    public static String getMsgByCode(int code) {
        CheckInStatusEnum status = CODE_MAP.get(code);
        return status != null ? status.getMsg() : "未知状态";
    }
}
