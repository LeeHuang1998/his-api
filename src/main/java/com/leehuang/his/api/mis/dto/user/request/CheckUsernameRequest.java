package com.leehuang.his.api.mis.dto.user.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class CheckUsernameRequest {

    @NotBlank(message = "username 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{5,20}$", message = "username 内容不正确")
    private String username;
}
