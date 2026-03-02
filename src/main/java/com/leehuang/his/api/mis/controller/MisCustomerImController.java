package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.front.dto.customerIM.vo.CustomerIMAccountVO;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.mis.dto.customerIm.vo.MisCustomerInfoVO;
import com.leehuang.his.api.mis.service.MisCustomerImService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/mis/customer/im")
@RequiredArgsConstructor
public class MisCustomerImController {

    private final MisCustomerImService misCustomerImService;

    /**
     * mis 端获取 客服 im 账号
     * @return
     */
    @GetMapping("/searchServiceImAccount")
    @SaCheckPermission(value = {"ROOT", "CUSTOMER_IM:SELECT"}, mode = SaMode.OR)
    public R searchServiceImAccount() {
        CustomerIMAccountVO imAccountVO = misCustomerImService.searchServiceImAccount();
        return R.OK().put("imAccountVO", imAccountVO);
    }

    @PostMapping("/searchCustomerSummary")
    @SaCheckLogin
    public R searchCustomerSummary(@RequestBody @Valid IdRequest request) {
        MisCustomerInfoVO customerImOrderVO = misCustomerImService.searchCustomerSummary(request.getId());
        return R.OK().put("customerImOrderVO", customerImOrderVO);
    }
}
