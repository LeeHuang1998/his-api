package com.leehuang.his.api.mis.dto.customerIm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatisticDTO {

    private Long totalCount;

    private BigDecimal totalAmount;

    private Integer number;
}
