package com.leehuang.his.api.front.dto.customer.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SmsCodeRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNum;

    @NotBlank(message = "区号不能为空")
    @Pattern(regexp = "^\\+\\d{1,5}$", message = "区号格式不正确")
    private String areaCode;
}
