package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.front.dto.customerIM.vo.CustomerIMAccountVO;
import com.leehuang.his.api.front.service.CustomerIMService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/customer/im")
@RequiredArgsConstructor
public class CustomerIMController {

    private final CustomerIMService customerIMService;

    @GetMapping("/createImAccount")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R createAccount() {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        CustomerIMAccountVO customerIMAccount = customerIMService.createCustomerIMAccount(customerId);
        return R.OK().put("imAccount", customerIMAccount);
    }
}
