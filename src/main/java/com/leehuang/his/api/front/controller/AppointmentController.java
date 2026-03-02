package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentPageRequest;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentRequest;
import com.leehuang.his.api.front.dto.appointment.vo.AppointmentPageVO;
import com.leehuang.his.api.front.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/front/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * 预约体检
     * @param request       请求参数
     * @return
     */
    @PostMapping("/insertAppointment")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R insertAppointment(@RequestBody @Valid AppointmentRequest request) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        String appointResult = appointmentService.insertAppointment(request, customerId);
        return R.OK().put("result", appointResult);
    }

    /**
     * front 分页查询预约记录
     * @param request
     * @return
     */
    @PostMapping("/searchAppointmentByPage")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R searchAppointmentByPage(@RequestBody @Valid AppointmentPageRequest request) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        PageUtils<AppointmentPageVO> pageData = appointmentService.searchAppointmentByPage(request, customerId);
        return R.OK().put("pageData", pageData);
    }
}
