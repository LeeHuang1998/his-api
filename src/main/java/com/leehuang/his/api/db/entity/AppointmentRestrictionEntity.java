package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * @TableName tb_appointment_restriction
 */
@Data
@TableName("tb_appointment_restriction")
public class AppointmentRestrictionEntity implements Serializable {
    private Integer id;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    // 实际每天的限制人数
    private Integer actualLimit;

    // 理论每天的限制人数，若没有设置 actualLimit 则使用该值
    private Integer everydayLimit;

    // 实际每天预约人数
    private Integer actualAppointment;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}