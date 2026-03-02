package com.leehuang.his.api.front.dto.customer.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class CustomerRequest {

    @NotBlank(message = "登录类型不能为空")
    @Pattern(regexp = "sms|password", message = "登录或注册，类型只能是 sms 或 password")
    private String type;

    @NotBlank(message = "标识不能为空")
    private String identity;                    // 用户名或手机号

    @NotBlank(message = "凭证不能为空")
    private String credential;                  // 密码或验证码

    private String areaCode;                    // 区号，短信登录时使用
}
