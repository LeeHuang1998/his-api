package com.leehuang.his.api.mis.dto.appointment.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class AppointmentCheckinRequest {

    @NotBlank(message = "pid 不能为空")
    @Pattern(regexp = "^[0-9Xx]{18}$", message = "身份证号码无效")
    private String pid;

    @NotBlank(message = "name 不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "name 内容不正确")
    private String name;

    @NotBlank(message = "photo_1 不能为空")
    private String photo_1;

    @NotBlank(message = "photo_2 不能为空")
    private String photo_2;
}
