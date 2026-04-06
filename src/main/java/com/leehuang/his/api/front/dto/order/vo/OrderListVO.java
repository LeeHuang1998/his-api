package com.leehuang.his.api.front.dto.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderListVO {

    private Integer id;

    private Integer goodsId;

    private String snapshotId;

    private String  goodsTitle;

    private String goodsDescription;

    private String goodsImage;

    private String outTradeNo;

    private Integer number;

    private Integer appointedNum;

    private BigDecimal goodsPrice;

    private BigDecimal payableAmount;

    private Integer status;

    /* disabled 字段用于判断该订单是否可以取消 */
    private Boolean disabled;

    private LocalDateTime createTime;
}
