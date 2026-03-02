package com.leehuang.his.api.mis.dto.flowRegulation.dto;

import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class CheckupProgressDTO {

    /** 体检人姓名 */
    private String name;

    /** 所有应体检的科室 */
    private Set<String> allPlaces;

    /** 已完成体检的科室 */
    private Map<String, String> finishedPlaces;

    /** 是否全部完成 */
    private boolean allFinished;
}
