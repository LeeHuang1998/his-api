package com.leehuang.his.api.mis.dto.flowRegulation.request;

import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Pattern;

@Data
public class FlowRegulationPageRequest {

//    /**
//     * 体检科室（搜索条件，可选），允许：数字、字母、中文、英文括号
//     */
//    @Pattern(regexp = "^[0-9a-zA-Z\\u4e00-\\u9fa5\\(\\)]{1,40}$", message = "place 内容不正确")
//    private String place;

    /**
     * 科室 id
     */
    @Min(value = 1, message = "id 不能小于 1")
    private Integer id;

    /**
     * 蓝标 UUID（搜索条件，可选），32 位字母数字
     */
    @Pattern(regexp = "^[0-9a-zA-Z]{32}$", message = "blueUuid 内容不正确")
    private String blueUuid;

    /**
     * 当前页码
     */
    @NotNull(message = "page 不能为空")
    @Min(value = 1, message = "page 不能小于1")
    private Integer page;

    /**
     * 每页条数
     */
    @NotNull(message = "length 不能为空")
    @Min(value = 10, message = "length 不能小于10")
    private Integer length;
}
