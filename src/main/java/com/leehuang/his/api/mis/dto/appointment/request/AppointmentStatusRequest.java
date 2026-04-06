package com.leehuang.his.api.mis.dto.appointment.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class AppointmentStatusRequest {

    @NotBlank(message = "uuid 不能为空")
    private String uuid;
}
