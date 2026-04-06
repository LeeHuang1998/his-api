package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_appointment
 */
@Data
@TableName("tb_appointment")
public class AppointmentEntity implements Serializable {
    private Integer id;

    private String uuid;

    private Integer orderId;

    private LocalDate appointmentDate;     // 预约的体检日期

    private String name;

    private String sex;

    private String pid;

    private LocalDate birthday;

    private String tel;

    private String appointmentDesc;         // 备注信息

    private Integer status;                 // 体检预约状态

    private LocalDateTime createTime;       // 体检预约创建时间

    private LocalDateTime checkinTime;      // 体检预约签到时间

    private LocalDateTime completedTime;    // 体检预约完成时间

    private Integer isDeleted;          // 是否删除：0-未删除，1-已删除

    private LocalDateTime deletedTime;  // 删除时间

    private static final long serialVersionUID = 1L;
}