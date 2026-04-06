package com.leehuang.his.api.front.dto.order.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class OrderAppointmentInfoVO {

    private Integer appointmentId;

    private Integer orderId;

    private String name;

    private String sex;

    private String tel;

    private String pid;

    private LocalDate appointmentDate;

    private LocalDateTime checkinTime;

    private String desc;

    private Integer status;
}
