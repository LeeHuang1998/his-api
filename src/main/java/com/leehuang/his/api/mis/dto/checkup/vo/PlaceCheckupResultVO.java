package com.leehuang.his.api.mis.dto.checkup.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PlaceCheckupResultVO {

    // 该体检项目体检时间
    private LocalDate checkupDate;

    // 医生姓名
    private String doctorName;

    // 医生 id
    private Integer doctorId;

    // 体检科室 id
    private Integer placeId;

    // 体检科室
    private String place;

    // 文档模板
    private String template;

    // 在该科室进行的所有的体检结果
    private List<PlaceCheckupResultItemVO> checkupItems;
}
