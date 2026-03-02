package com.leehuang.his.api.mis.dto.dept.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;

@Data
public class DeptRequest {

    @Null(groups = {Insert.class}, message = "deptId 必须为空")
    @NotNull(groups = {Update.class}, message = "dpetId 不能为空")
    @Min(groups = {Update.class}, value = 1,message = "deptId 必须大于 0")
    private Integer id;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    @Pattern(regexp = "^1[1-9]\\d{9}$|^(0\\d{2,3}\\-){0,1}[1-9]\\d{6,7}$", message = "联系电话格式不正确")
    private String tel;

    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    @Length(max = 50, message = "备注不能超过 50 个字符")
    private String desc;
}
