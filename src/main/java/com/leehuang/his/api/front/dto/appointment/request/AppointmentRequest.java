package com.leehuang.his.api.front.dto.appointment.request;


import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class AppointmentRequest {

    @NotNull(message = "orderId 不能为空")
    @Min(value = 1, message = "orderId 不能小于1")
    private Integer orderId;

    @NotNull(message = "预约日期不能为空")
    private LocalDate appointmentDate;

    @NotBlank(message = "预约体检人不能为空")
    private String name;

    @NotBlank(message = "手机号不能为空")
    private String tel;

    @NotBlank(message = "pid不能为空")
    @Pattern(regexp = "^[0-9Xx]{18}$", message = "pid内容不正确")
    private String pid;

    @Size(min = 1, max = 100, message = "备注长度不能超过 100")
    private String desc;

}
