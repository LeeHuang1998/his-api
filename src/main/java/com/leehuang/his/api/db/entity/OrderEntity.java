package com.leehuang.his.api.db.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

/**
 * @TableName tb_order
 */
@Data
@TableName("tb_order")
public class OrderEntity implements Serializable {
    private Integer id;

    private Integer customerId;

    private Integer goodsId;

    private String snapshotId;

    private Integer addressId;

    private String goodsTitle;

    private BigDecimal goodsPrice;

    private Integer number;

    private BigDecimal payableAmount;

    private BigDecimal discountAmount;

    private BigDecimal totalAmount;

    private String goodsImage;

    private String goodsDescription;

    private String orderNotes;

    private String outTradeNo;

    private String paymentType;

    private String transactionId;

    private String outRefundNo;

    private BigDecimal refundAmount;

    private Integer status;

    private String createDate;

    private String createTime;

    private String updateTime;

    private String refundDate;

    private String refundTime;

    @Version
    private Integer version;

    private static final long serialVersionUID = 1L;
}