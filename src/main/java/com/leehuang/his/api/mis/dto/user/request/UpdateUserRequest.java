package com.leehuang.his.api.mis.dto.user.request;

import lombok.Data;

import javax.validation.constraints.*;

@Data
public class UpdateUserRequest {

    @NotNull(message = "userId 不能为空")
    @Min(value = 1,message = "userId 必须大于 0")
    private Integer id;

    @NotBlank(message = "username 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{5,20}$", message = "username 内容不正确")
    private String username;

    @NotBlank(message = "name 不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "name 内容不正确")
    private String name;

    @NotBlank(message = "sex 不能为空")
    @Pattern(regexp = "^男$|^女$", message = "sex 内容不正确")
    private String sex;

    @NotBlank(message = "tel 不能为空")
    @Pattern(regexp = "^1[1-9]\\d{9}$", message = "tel 内容不正确")
    private String tel;

    @NotBlank(message = "email 内容不正确")
    @Email(message = "email 内容不正确")
    private String email;

    @NotBlank(message = "hiredate 不能为空")
    @Pattern(regexp = "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29))$", message = "hiredate 内容不正确")
    private String hiredate;

    @NotEmpty(message = "role 不能为空")
    private Integer[] role;

    @Min(value = 1, message = "deptId 不能小于1")
    private Integer deptId;
}
