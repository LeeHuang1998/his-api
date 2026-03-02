package com.leehuang.his.api.exception;

public enum BizCodeEnum {

    EMPTY_TOKEN_Exception(4001,"缺少 token"),
    PERMISSION_EXCEPTION(4002,"权限错误"),

    SMS_CODE_ERROR(4003,"验证码错误"),
    SMS_CODE_EXPIRE(4004,"验证码已过期"),

    USER_NOT_EXIST(4005,"用户不存在"),
    USER_EXIST(4006,"用户已存在"),
    PASSWORD_ERROR(4007,"密码错误"),
    // 该用户已被绑定
    USER_BIND_EXIST(4008,"该用户已被绑定"),

    UPLOAD_EXCEPTION(6001),
    DOWNLOAD_EXCEPTION(6002),
    HIS_EXCEPTION(500);

    private Integer code;
    private String msg;


    BizCodeEnum(Integer code) {
        this.code = code;
    }

    BizCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
