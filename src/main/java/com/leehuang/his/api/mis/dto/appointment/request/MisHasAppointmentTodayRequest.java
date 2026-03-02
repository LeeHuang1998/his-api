package com.leehuang.his.api.mis.dto.appointment.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class MisHasAppointmentTodayRequest {

    @NotBlank(message = "name不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{1,10}$", message = "name 内容不正确")
    private String name;

    @NotBlank(message = "sex不能为空")
    @Pattern(regexp = "^男$|^女$", message = "sex内容不正确")
    private String sex;

    @NotBlank(message = "pid不能为空")
    @Pattern(regexp = "^[0-9Xx]{18}$", message = "身份证号码无效")
    private String pid;
}
