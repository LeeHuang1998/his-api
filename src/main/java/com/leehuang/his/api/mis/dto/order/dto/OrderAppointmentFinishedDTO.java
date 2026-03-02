package com.leehuang.his.api.mis.dto.order.dto;

import lombok.Data;

@Data
public class OrderAppointmentFinishedDTO {

    private Integer orderId;

    private Integer goodsCount;

    private Integer appointmentFinishedCount;
}
