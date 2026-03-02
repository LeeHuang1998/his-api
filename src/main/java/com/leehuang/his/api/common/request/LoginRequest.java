package com.leehuang.his.api.common.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class LoginRequest {

    @NotBlank(message = "username 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{5,50}$", message = "username 格式不正确")
    private String username;

    @NotBlank(message = "password 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "password 格式不正确")
    private String password;
}
