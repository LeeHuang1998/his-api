package com.leehuang.his.api.front.service;

import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;

public interface AlipayService {

    // 支付宝订单查询
    AlipayTradeQueryResponse queryOrder(String outTradeNo);

    // 生成支付宝二维码
    String alipayQrcode(String outTradeNo, String totalAmount, String subject);

    // 关闭支付宝订单
    void closeOrder(String outTradeNo);

    // 支付宝退款
    AlipayTradeRefundResponse refund(String outTradeNo, String refundAmount, String refundReason, String outRefundNo);

    // 查询退款结果
    AlipayTradeFastpayRefundQueryResponse queryRefund(String outTradeNo, String outRefundNo);
}
