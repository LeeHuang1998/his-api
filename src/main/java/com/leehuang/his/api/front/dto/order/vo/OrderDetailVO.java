package com.leehuang.his.api.front.dto.order.vo;

import com.leehuang.his.api.front.dto.address.vo.AddressVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDetailVO {

    private String outTradeNo;                          // 订单流水号

    private AddressVO addressVO;                        // 收货地址

    private OrderGoodsVO goodsVO;                       // 商品信息

    private Integer number;                             // 商品数量

    private String paymentType;                             // 支付方式

    private String orderNotes;                          // 订单备注

    private BigDecimal orderTotalAmount;                // 订单总金额

    private BigDecimal discountAmount;                  // 折扣金额

    private BigDecimal payableAmount;                   // 应付金额

    private String createTime;                          // 创建时间

    private Integer status;                             // 订单状态
}
