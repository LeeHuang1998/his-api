package com.leehuang.his.api.mis.dto.appointment.vo;

import cn.hutool.core.util.IdcardUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Data
public class AppointmentVO {
    private Integer id;

    private String name;

    private LocalDate appointmentDate;

    private String sex;

    private String tel;

    private String pid;

    private Integer age;

    private String desc;

    private Integer status;

    private String goodsTitle;

    private String snapShotId;

    // pid 脱敏
    public String getPid() {
        if (pid == null || pid.length() < 18) return pid;
        return pid.substring(0, 3) + "***********" + pid.substring(14);
    }

    // 年龄计算
    public Integer getAge() {
        if (pid == null || pid.length() < 18) return null;
        try {
            String birthStr = pid.substring(6, 14);
            LocalDate birthday = LocalDate.parse(birthStr, DateTimeFormatter.BASIC_ISO_DATE);
            return Period.between(birthday, LocalDate.now()).getYears();
        } catch (Exception e) {
            return null;
        }
    }
}
