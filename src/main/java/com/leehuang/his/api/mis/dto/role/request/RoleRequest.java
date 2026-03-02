package com.leehuang.his.api.mis.dto.role.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;

@Data
public class RoleRequest {

    @Null(groups = {Insert.class}, message = "id 在插入时必须为空")
    @NotNull(groups = {Update.class}, message = "角色 id 不能为空")
    @Min(groups = {Update.class}, value = 1, message = "角色 id 不能小于 1")
    private Integer id;

    @NotBlank(message = "角色名不能为空")
    private String roleName;

    @NotEmpty(message = "权限不能为空")
    private Integer[] permissions;

    @Length(max = 50, message = "备注长度不能超过 50 个字符")
    private String desc;

    @Null(groups = {Insert.class}, message = "changed 在插入时必须为空")
    @NotNull(groups = {Update.class}, message = "changed 不能为空")
    private Boolean changed;
}
