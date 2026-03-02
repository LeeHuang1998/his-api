package com.leehuang.his.api.mis.dto.appointment.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderPageAppointmentVO {

    private Integer id;

    private String name;

    private String sex;

    private String tel;

    private Integer age;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDate appointmentDate;

    private Integer status;
}
