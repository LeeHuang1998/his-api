package com.leehuang.his.api.mis.dto.appointment.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GuidanceSummaryInfoVO {

    private String uuid;

    private Integer orderId;

    private String snapshotId;

    private String name;

    private String sex;

    private LocalDate birthday;

    private Integer age;

    private String pid;

    private String tel;

    private LocalDate appointmentDate;

    private String desc;
}
