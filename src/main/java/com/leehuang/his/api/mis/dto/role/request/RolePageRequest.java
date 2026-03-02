package com.leehuang.his.api.mis.dto.role.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class RolePageRequest {

    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{1,10}", message = "roleName 名称只能包含 1-10 个中文字符")
    private String roleName;

    @NotNull(message = "page 页码不能为空")
    @Min(value = 1, message = "page 页码不能小于 1")
    private Integer page;

    @NotNull(message = "length 每页条数不能为空")
    @Range(min = 10, max = 50, message = "length 每页条数不能小于 10 大于 50")
    private Integer length;
}
