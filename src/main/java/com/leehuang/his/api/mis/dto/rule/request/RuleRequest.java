package com.leehuang.his.api.mis.dto.rule.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;

@Data
public class RuleRequest {

    @Null(message = "插入时 id 必须为空", groups = {Insert.class})
    @NotNull(message = "更新时 id 不能为空", groups = {Update.class})
    private Integer id;

    @NotBlank(message = "name 不能为空")
    @Pattern(regexp = "^[0-9a-zA-Z\\u4e00-\\u9fa5]{1,20}$", message = "name 内容不正确")
    private String name;

    @NotBlank(message = "rule不能为空")
    private String rule;

    private String remark;
}
