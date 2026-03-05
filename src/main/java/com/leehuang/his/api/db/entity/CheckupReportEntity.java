package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @TableName tb_checkup_report
 */
@Data
@TableName("tb_checkup_report")
public class CheckupReportEntity implements Serializable {
    private Integer id;

    private Integer appointmentId;

    private String resultId;

    private Integer status;

    private String filePath;

    private String waybillCode;

    private String waybillDate;

    private LocalDate date;

    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}