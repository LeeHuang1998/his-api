package com.leehuang.his.api.mis.dto.flowRegulation.dto;

import lombok.Data;

@Data
public class NextPlaceVO {

    private Integer id;

    private String place;

    private Integer waitingCount;
}