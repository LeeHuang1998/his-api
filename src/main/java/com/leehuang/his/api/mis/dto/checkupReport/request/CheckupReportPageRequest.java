package com.leehuang.his.api.mis.dto.checkupReport.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class CheckupReportPageRequest {

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5.·]{0,20}$", message = "name 格式不正确")
    private String name;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "tel 内容不正确")
    private String tel;

    @Pattern(regexp = "^$|^[0-9A-Za-z]{10,24}$", message = "waybillCode 内容不正确")
    private String waybillCode;

    @Range(min = 1, max = 4, message = "status 内容不正确")
    private Integer status;

    @NotNull(message = "page 不能为空")
    @Min(value = 1, message = "page不能小于1")
    private Integer page;

    @NotNull(message = "length 不能为空")
    @Range(min = 10, max = 50, message = "length 必须为 10~50 之间")
    private Integer length;
}
