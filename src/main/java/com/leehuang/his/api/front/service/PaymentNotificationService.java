package com.leehuang.his.api.front.service;

public interface PaymentNotificationService {

    void sendPaymentResult(String outTradeNo, String status);
}
