package com.leehuang.his.api.front.dto.customer.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.front.dto.customer.request.validator.ThirdPartyBind;
import com.leehuang.his.api.front.dto.customer.request.validator.ThirdPartyRegister;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ThirdPartyRequest {
    // 第三方平台用户信息
    private String nickname;

    @NotBlank(message = "openId 不能为空")
    private String openId;

    @NotBlank(message = "platform 不能为空")
    private String platform;

    private String email;

    private String avatar;

    // 本平台注册信息
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNum;

    @NotBlank(groups = {ThirdPartyRegister.class}, message = "password 不能为空")
    @Null(groups = {ThirdPartyBind.class}, message = "password 必须为空")
    @Size(groups = {ThirdPartyBind.class}, max = 0, message = "password 必须为空字符串或 null")
    private String password;

    @NotBlank(message = "区号不能为空")
    @Pattern(regexp = "^\\+\\d{1,5}$", message = "区号格式不正确")
    private String smsCode;

    private String areaCode;

}
