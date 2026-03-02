package com.leehuang.his.api.front.dto.appointment.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentPageVO {

    private Integer id;

    private String goodsTitle;

    private String name;

    private LocalDate appointmentDate;

    private Integer status;

    private String filePath;

}
