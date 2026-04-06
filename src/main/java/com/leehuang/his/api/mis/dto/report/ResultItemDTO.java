package com.leehuang.his.api.mis.dto.report;

import lombok.Data;

@Data
public class ResultItemDTO {

    // 检查项目名称
    private String checkupName;
    // 检查结果值
    private String value;
    // 单位
    private String unit;
    // 参考范围
    private String standard;
}
