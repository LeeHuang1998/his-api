package com.leehuang.his.api.front.service;

import com.leehuang.his.api.front.dto.pay.request.PayRequest;

import javax.servlet.http.HttpServletRequest;

public interface PaymentService {

    // 生成支付 QRCode
    String createQRCode(PayRequest request);

    // 支付结果回调处理
    String handleNotify(HttpServletRequest request);
}
