package com.leehuang.his.api.common.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum OrderStatusEnum {

    /**
     * 订单状态码
     */
    UNPAID(1, "未付款"),
    CLOSED(2, "已关闭"),
    PAID(3, "已付款"),
    REFUNDED(4, "已退款"),
    APPOINTED(5, "已预约"),
    FINISHED(6, "已结束"),
    REFUNDING(7, "退款中"),
    REFUND_FAILED(8,"退款失败");

    private final Integer code;
    private final String msg;

    OrderStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    // 使用 Map 缓存，O(1) 时间复杂度查找
    private static final Map<Integer, OrderStatusEnum> CODE_MAP = new HashMap<>();

    // 类首次被加载时，自动将所有枚举实例存入 CODE_MAP 缓存
    static {
        for (OrderStatusEnum status : values()) {
            CODE_MAP.put(status.getCode(), status);
        }
    }

    /**
     * 根据 code 获取枚举
     * @param code  状态码
     * @return      对应的枚举对象，不存在时返回 null
     */
    public static OrderStatusEnum fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据 code 直接获取 msg（推荐）
     * @param code 状态码
     * @return 对应的描述，不存在时返回默认值
     */
    public static String getMsgByCode(int code) {
        OrderStatusEnum status = CODE_MAP.get(code);
        return status != null ? status.getMsg() : "未知状态";
    }

}
