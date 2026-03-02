package com.leehuang.his.api.common;

import org.springframework.http.HttpStatus;

import java.util.HashMap;

public class R extends HashMap<String, Object> {

    public R(){
        // 默认创建的 R 对象中包含了公共的属性
        put("code", HttpStatus.OK.value());
        put("msg", "success");
    }

    // 覆盖继承自父类的 put 方法，作为自己的 put 方法，添加 key-value，并实现链式调用
    public R put(String key, Object value) {
        super.put(key, value);
        return this;                    // 返回自己实现链式调用
    }

    // 自定义 OK 方法，表示成功返回
    public static R OK(){
        return new R();                 // 直接返回对象即可，构造器中设置了默认的属性值
    }

    // 自定义返回的 OK 信息（msg）
    public static R OK(String msg){
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    // 自定义返回的 OK 信息（Map）
    public static R OK(HashMap<String, Object> map){
        R r = new R();
        r.putAll(map);
        return r;
    }

    // 自定义返回的错误信息
    public static R error(Integer code, String msg){
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    // 自定义返回的错误信息（500）
    public static R error(String msg){
        return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg);
    }

    // 自定义返回的错误信息
    public static R error(){
        return R.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "未知异常，请联系管理员");
    }

}
