package com.leehuang.his.api.mis.dto.checkupReport.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CheckupReportPageVO {
    private Integer reportId;

    private Integer appointmentId;

    private String name;

    private String sex;

    private String tel;

    private LocalDate birthday;

    private Integer age;

    private Integer appointmentStatus;

    private LocalDate appointmentDate;

    private String resultId;

    /** 检查报告的状态 */
    private Integer status;

    private LocalDateTime generatedTime;

    private String waybillCode;

    private LocalDate waybillDate;
}
