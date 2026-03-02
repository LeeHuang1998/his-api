package com.leehuang.his.api.mis.dto.user.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class UserPageRequest {

    @NotNull(message = "page 页码不能为空")
    @Min(value = 1, message = "page 页码不能小于 1")
    private Integer page;

    @NotNull(message = "length 每页条数不能为空")
    @Range(min = 10, max = 50, message = "length 每页条数不能小于 10 大于 50")
    private Integer length;

    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{1,10}", message = "name 名称只能包含 1-10 个中文字符")
    private String name;

    @Pattern(regexp = "^男$|^女$", message = "sex 性别内容不正确")
    private String sex;

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,10}", message = "role 角色只能包含 2-10 个中文、英文字符")
    private String role;

    @Min(value = 1, message = "deptId 不能小于 1")
    private Integer deptId;

    @Min(value = 1, message = "status 不能小于 1")
    private Integer status;
}
