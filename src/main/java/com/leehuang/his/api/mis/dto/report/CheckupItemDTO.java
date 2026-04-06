package com.leehuang.his.api.mis.dto.report;

import lombok.Data;

@Data
public class CheckupItemDTO {

    // 体检科室
    private String place;
    // 体检项目名称
    private String item;
}
