package com.leehuang.his.api.mis.dto.user.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "原密码格式不正确")
    private String password;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,20}$", message = "新密码格式不正确")
    private String newPassword;
}
