package com.leehuang.his.api.front.dto.appointment.request;

import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Data
public class AppointmentPageRequest {

    private Integer customerId;

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{1,50}$", message = "keyword 内容不正确")
    private String keyword;

    @Pattern(regexp = "^1$|^4$", message = "status 内容不正确")
    private String status;

    private LocalDate appointmentDate;

    @NotNull(message = "page 不能为空")
    @Min(value = 1, message = "page 不能小于1")
    private Integer page;

    @NotNull(message = "length不能为空")
    @Range(min = 10, max = 50, message = "length必须为10~50之间")
    private Integer length;
}
