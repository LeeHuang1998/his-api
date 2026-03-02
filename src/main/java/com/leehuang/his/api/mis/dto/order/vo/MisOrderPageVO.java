package com.leehuang.his.api.mis.dto.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class MisOrderPageVO {

    private int id;

    private String goodsTitle;

    private BigDecimal goodsPrice;

    private String snapshotId;

    private Integer number;

    private BigDecimal totalAmount;

    private String photo;

    private String name;

    private String sex;

    private String tel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime registerTime;

    private Integer status;

    private String outTradeNo;

    private String transactionId;

    private String outRefundNo;

    private LocalDate createDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    private LocalDate refundDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime refundTime;

    private Integer appointmentNum;                // 预约数量
}
