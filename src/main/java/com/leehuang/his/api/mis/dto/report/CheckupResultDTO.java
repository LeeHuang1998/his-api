package com.leehuang.his.api.mis.dto.report;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CheckupResultDTO {

    // 科室名称
    private String place;
    // 体检医生
    private String doctorName;
    // 报告日期
    private LocalDate date;
    // 图片URL
    private String image;
    // 模板类型（模板1 / 模板2）
    private String template;
    // 具体的检查项目结果列表
    private List<ResultItemDTO> resultItems;
}
