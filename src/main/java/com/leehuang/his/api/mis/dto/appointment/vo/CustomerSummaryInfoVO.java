package com.leehuang.his.api.mis.dto.appointment.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerSummaryInfoVO {

//    private Integer orderId;

    private String name;

    private String sex;

    private LocalDate birthday;

    private Integer age;

    private String tel;

    private String uuid;

    private String pid;

    private LocalDate appointmentDate;

    private LocalDateTime checkinTime;

    private Integer status;

    private String snapshotId;
}
