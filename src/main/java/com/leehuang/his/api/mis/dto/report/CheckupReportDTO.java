package com.leehuang.his.api.mis.dto.report;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CheckupReportDTO {

    // 唯一标识（用于二维码）
    private String uuid;
    // 姓名
    private String name;
    // 性别
    private String sex;
    // 电话
    private String tel;
    // 出生日期
    private LocalDate birthday;
    // 年龄
    private Integer age;
    // 收货地址
    private String address;
    // 体检日期
    private LocalDate appointmentDate;
    // 体检套餐名称
    private String goods;
    // 体检项目列表
    private List<CheckupItemDTO> checkup;
    // 体检结果列表
    private List<CheckupResultDTO> result;
}
