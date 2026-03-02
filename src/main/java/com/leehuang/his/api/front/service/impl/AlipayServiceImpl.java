package com.leehuang.his.api.front.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.leehuang.his.api.config.AlipayConfig;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("AlipayService")
@RequiredArgsConstructor
@Slf4j
public class AlipayServiceImpl implements AlipayService {

    private final AlipayConfig alipayConfig;

    /**
     * 查询支付宝订单
     * @param outTradeNo    订单流水号
     * @return  查询响应
     */
    @Override
    public AlipayTradeQueryResponse queryOrder(String outTradeNo) {
        try {
            AlipayClient client = alipayConfig.getAlipayClient();

            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

            Map<String, String> bizMap = new HashMap<>();
            bizMap.put("out_trade_no", outTradeNo);
            String bizContent = JSON.toJSONString(bizMap);

            request.setBizContent(bizContent);

            // response 若为 null 则会直接被捕获
            AlipayTradeQueryResponse response = client.execute(request);

            if (!response.isSuccess()) {
                String subCode = response.getSubCode();

                // 订单不存在、已关闭等，是可以接收的业务状态，视为无待支付订单，直接返回即可
                if ("ACQ.TRADE_NOT_EXIST".equals(subCode) ||
                        "ACQ.TRADE_HAS_CLOSE".equals(subCode)) {
                    return response;
                }

                log.warn("支付宝查询订单未成功 outTradeNo={}, code={}, msg={}, subCode = {}",
                        outTradeNo, response.getCode(), response.getMsg(), subCode);

                if ("ACQ.SYSTEM_ERROR".equals(subCode)) {
                    throw new HisException("支付宝系统异常");
                }
            }
            return response;
        } catch (Exception e) {
            log.error("支付宝查询订单异常，订单号: {}", outTradeNo, e);
            throw new HisException("支付宝查询订单异常：" + e.getMessage());
        }
    }

    /**
     * 支付宝预下单创建 qrcode
     * @param outTradeNo        订单号
     * @param totalAmount       总金额
     * @param subject           订单标题
     * @return
     */
    @Override
    public String alipayQrcode(String outTradeNo, String totalAmount, String subject) {
        // 1. 初始化客户端
        AlipayClient alipayClient = alipayConfig.getAlipayClient();

        // 2. 构建请求
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();

        // 3. 设置业务参数（JSON 字符串）
        Map<String, String> bizMap = new HashMap<>();
        bizMap.put("out_trade_no", outTradeNo);
        bizMap.put("total_amount", totalAmount);
        bizMap.put("subject", subject);
        bizMap.put("timeout_express", "2h");            // 二维码过期时间

        String bizContent = JSON.toJSONString(bizMap);
        request.setBizContent(bizContent);

        // 4. 设置异步通知地址
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        try {
            // 5. 执行请求
            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            if ("10000".equals(response.getCode()) && response.isSuccess()) {
                // 生成二维码成功
                String qrCodeString = response.getQrCode();
                log.info("支付宝预下单成功，订单号: {}, 二维码: {}", outTradeNo, qrCodeString);
                // 返回二维码链接
                return qrCodeString;
            } else {
                String errorMsg = String.format("支付宝预下单失败，订单号: %s, 错误码: %s, 错误信息: %s",
                        outTradeNo, response.getCode(), response.getSubMsg());
                log.error(errorMsg);
                throw new HisException(errorMsg);
            }
        } catch (Exception e) {
            // 传入异常对象，会自动记录堆栈
            log.error("调用支付宝接口异常，订单号: {}", outTradeNo, e);
            throw new HisException("调用支付宝接口异常", e);
        }
    }

    /**
     * 支付宝关闭订单
     * @param outTradeNo    订单流水号
     */
    @Override
    public void closeOrder(String outTradeNo) {
        try {
            AlipayClient client = alipayConfig.getAlipayClient();

            AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

            Map<String, String> bizMap = new HashMap<>();
            bizMap.put("out_trade_no", outTradeNo);
            String bizContent = JSON.toJSONString(bizMap);

            request.setBizContent(bizContent);

            // response 若为 null 则会直接被捕获
            AlipayTradeCloseResponse response = client.execute(request);

            if (!response.isSuccess()) {
                log.warn("支付宝关单失败 outTradeNo={}, msg={}", outTradeNo, response.getSubMsg());
                throw new HisException("支付宝关闭订单失败：" + response.getSubMsg());
            }

        } catch (Exception e) {
            throw new HisException("支付宝关闭订单异常：" + e.getMessage());
        }
    }

    /**
     * 支付宝退款
     * @param outTradeNo            订单流水号
     * @param refundAmount          订单金额
     * @param refundReason          退款原因
     * @return
     */
    @Override
    public AlipayTradeRefundResponse refund(String outTradeNo, String refundAmount, String refundReason, String outRefundNo) {
        try {
            AlipayClient client = alipayConfig.getAlipayClient();
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            Map<String, String> bizMap = new HashMap<>();
            bizMap.put("out_trade_no", outTradeNo);
            bizMap.put("refund_amount", refundAmount); // 退款金额，单位元，如 "0.01"
            bizMap.put("refund_reason", refundReason != null ? refundReason : "用户申请退款");
            // 添加商户退款请求号，用于幂等控制
            bizMap.put("out_request_no", outRefundNo);

            request.setBizContent(JSON.toJSONString(bizMap));

            AlipayTradeRefundResponse response = client.execute(request);

            if (!response.isSuccess()) {
                log.warn("支付宝退款失败 outTradeNo={}, subCode={}, subMsg={}",
                        outTradeNo, response.getSubCode(), response.getSubMsg());
                throw new HisException("退款失败：" + response.getSubMsg());
            }

            log.info("支付宝退款成功 outTradeNo={}, refundAmount={}, tradeNo={}",
                    outTradeNo, refundAmount, response.getTradeNo());
            return response;

        } catch (AlipayApiException e) {
            log.error("支付宝退款 API 异常，订单号: {}", outTradeNo, e);
            throw new HisException("退款接口调用异常：" + e.getErrMsg());
        } catch (Exception e) {
            log.error("支付宝退款系统异常，订单号: {}", outTradeNo, e);
            throw new HisException("退款异常：" + e.getMessage());
        }
    }

    /**
     * 查询退款结果
     * @param outTradeNo             订单流水号
     * @param outRefundNo            退款流水号
     * @return
     */
    @Override
    public AlipayTradeFastpayRefundQueryResponse queryRefund(String outTradeNo, String outRefundNo) {
        try {
            AlipayClient alipayClient = alipayConfig.getAlipayClient();

            AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();

            Map<String, String> bizMap = new HashMap<>();
            bizMap.put("out_trade_no", outTradeNo);
            bizMap.put("out_request_no", outRefundNo);

            request.setBizContent(JSON.toJSONString(bizMap));

            AlipayTradeFastpayRefundQueryResponse response = alipayClient.execute(request);

            if (response.isSuccess()) {
                log.info("退款查询成功: refund_status={}, refund_amount={}, gmt_refund_pay={}",
                        response.getRefundStatus(),
                        response.getRefundAmount(),
                        response.getGmtRefundPay());
                return response;
            } else {
                log.error("退款查询失败: code={}, msg={}, sub_code={}, sub_msg={}",
                        response.getCode(),
                        response.getMsg(),
                        response.getSubCode(),
                        response.getSubMsg());
                throw new HisException("支付宝退款查询失败: " + response.getSubMsg());
            }
        } catch (Exception e) {
            log.error("调用支付宝退款查询接口异常", e);
            throw new RuntimeException("支付宝退款查询异常", e);
        }
    }
}
