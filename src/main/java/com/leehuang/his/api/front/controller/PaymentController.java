package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.front.dto.pay.request.PayRequest;
import com.leehuang.his.api.front.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/front/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 生成支付 QRCODE
     * @param request   支付请求
     * @return
     */
    @PostMapping("/createQrCode")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R createQRCode(@Valid @RequestBody PayRequest request) {
        String qrCode = paymentService.createQRCode(request);
        return R.OK().put("qrCode", qrCode);
    }

    /**
     * 支付宝异步通知地址（必须公网可访问），支付成功后，支付宝会 POST 请求此接口
     * @param request
     * @return  支付结果
     */
    @PostMapping("/notify")
    public String handleNotify(HttpServletRequest request) {
        return paymentService.handleNotify(request);
    }

    @GetMapping("/testNatapp")
    public R testNatapp() {
        return R.OK("测试成功");
    }
}
