package com.leehuang.his.api.front.dto.customer.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class CustomerInfoRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,15}$", message = "用户名格式不正确")
    private String username;

    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z·\\s]{2,30}$", message = "姓名格式不正确")
    private String name;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^([男女])$", message = "性别错误")
    private String sex;

    @Email(message = "email 内容不正确")
    private String email;
}
