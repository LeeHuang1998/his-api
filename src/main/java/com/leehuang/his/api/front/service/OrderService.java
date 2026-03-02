package com.leehuang.his.api.front.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.front.dto.order.request.OutTradeNoRequest;
import com.leehuang.his.api.front.dto.order.request.OrderRequest;
import com.leehuang.his.api.front.dto.order.request.RefundOrderRequest;
import com.leehuang.his.api.front.dto.order.vo.OrderDetailVO;
import com.leehuang.his.api.front.dto.order.vo.OrderListVO;

import java.math.BigDecimal;

public interface OrderService {

    // 创建订单
    String createOrder(OrderRequest request);

    // 查询订单详情
    OrderDetailVO getOrderDetail(String outTradeNo);

    // 更新订单状态
    Boolean updateOrderStatus(String outTradeNo, String tradeNo, Integer expectStatus, Integer targetStatus);

    // 检查订单状态
    Integer checkOrderStatus(String outTradeNo);

    // 获取客户 id
    Integer searchCustomerId(String outTradeNo);

    // 分页查询订单列表
    PageUtils<OrderListVO> searchOrderListByCustomerId(
            Integer page,
            Integer length,
            String keyword,
            Integer status,
            Integer customerId
    );

    // 更新支付方式和地址 id
    void updatePaymentType(String outTradeNo, Integer addressId, String paymentType);

    // 获取订单的实际支付金额
    BigDecimal getPayableAmount(String outTradeNo);

    // 获取订单支付信息
    OrderEntity getOrderPayInfo(String outTradeNo);

    // 关闭订单
    String closeOrder(OutTradeNoRequest request);

    // 批量关闭订单
    int batchCloseExpiredOrder(int overdueMinutes, int batchSize);

    // 订单退款
    void refundOrder(RefundOrderRequest request, Integer customerId);

    // 查询退款
    String checkRefund(OutTradeNoRequest request);
}
