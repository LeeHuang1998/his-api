package com.leehuang.his.api.mis.dto.appointment.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AppointmentStatusRequest {

    @NotBlank(message = "uuid 不能为空")
    private String uuid;

    @NotNull(message = "newStatus 不能为空")
    @Range(min = 1, max = 4, message = "newStatus 内容不正确")
    private Integer newStatus;
}
