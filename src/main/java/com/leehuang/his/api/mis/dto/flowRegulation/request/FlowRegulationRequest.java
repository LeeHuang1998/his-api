package com.leehuang.his.api.mis.dto.flowRegulation.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;

@Data
public class FlowRegulationRequest {

    @Null(groups = {Insert.class}, message = "插入时 id 必须为空")
    @NotNull(groups = {Update.class},message = "更新时 id 不能为空")
    private Integer id;

    @NotBlank(message = "place 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,40}$", message = "place 内容不正确")
    private String place;

    @NotBlank(message = "blueUuid 不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{32}$", message = "blueUuid 内容不正确")
    private String blueUuid;

    @NotNull(message = "maxNum 不能为空")
    @Range(min = 1, max = 1000, message = "maxNum 内容不正确")
    private Integer maxNum;

    @NotNull(message = "weight 不能为空")
    @Range(min = 1, max = 10, message = "weight 内容不正确")
    private Integer weight;

    @NotNull(message = "priority 不能为空")
    @Range(min = 1, max = 10, message = "priority 内容不正确")
    private Integer priority;
}
