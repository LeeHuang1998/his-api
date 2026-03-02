package com.leehuang.his.api.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义异常
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class HisException extends RuntimeException {
    private String msg;
    private int code = 500;

    public HisException(Exception e) {
        // 调用父类构造器，将异常包装为 HisException 对象，封装执行栈信息到异常对象中
        super(e);
        this.msg = "执行异常";
    }

    public HisException(String msg) {
        super(msg);
        this.msg = msg;
    }

    // Throwable 可以派生出 Exception 和 Error 两个子类，无论是传入 Exception 还是 Error，都会被包装为 HisException，封装错误信息到异常对象中
    public HisException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public HisException(int code, String msg) {
        super(msg); 
        this.msg = msg;
        this.code = code;
    }

    public HisException(int code, String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

}
