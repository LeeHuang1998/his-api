package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.mis.dto.appointment.request.AppointmentCheckinRequest;
import com.leehuang.his.api.mis.dto.appointment.request.AppointmentStatusRequest;
import com.leehuang.his.api.mis.dto.appointment.request.MisAppointmentPageRequest;
import com.leehuang.his.api.mis.dto.appointment.request.MisHasAppointmentTodayRequest;
import com.leehuang.his.api.mis.dto.appointment.vo.*;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.mis.service.MisAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController("MisAppointmentController")
@RequestMapping("/mis/appointment")
@RequiredArgsConstructor
public class MisAppointmentController {

    private final MisAppointmentService misAppointmentService;

    /**
     * 根据订单 id 查询预约信息
     * @param request
     * @return
     */
    @PostMapping("/searchAppointmentByOrderId")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:SELECT"}, mode = SaMode.OR)
    public R searchByOrderId(@RequestBody @Valid IdRequest request) {
        List<OrderPageAppointmentVO> appointmentVOList = misAppointmentService.searchByOrderId(request.getId());
        return R.OK().put("appointmentVOList", appointmentVOList);
    }

    /**
     * mis 端分页查询预约数据
     * @param request
     * @return
     */
    @PostMapping("/searchAppointmentByPageForMis")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:SELECT"}, mode = SaMode.OR)
    public R searchAppointmentByPageForMis(@RequestBody @Valid MisAppointmentPageRequest request) {
        PageUtils<AppointmentVO> pageData = misAppointmentService.searchAppointmentByPageForMis(request);
        return R.OK().put("pageData", pageData);
    }

    /**
     * 批量删除预约信息
     * @param request
     * @return
     */
    @PostMapping("/deleteAppointmentByIds")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:DELETE"}, mode = SaMode.OR)
    public R deleteAppointmentByIds(@RequestBody @Valid IdsRequest request) {
        int rows = misAppointmentService.deleteAppointmentByIds(request.getIds());
        return R.OK().put("rows", rows);
    }

    /**
     * 查询今日是否有预约
     * @param request
     * @return
     */
    @PostMapping("/hasAppointmentInToday")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:UPDATE"}, mode = SaMode.OR)
    public R hasAppointmentInToday(@RequestBody @Valid MisHasAppointmentTodayRequest request) {
        int result = misAppointmentService.hasAppointInToday(request);
        return R.OK().put("appointmentResult", result);
    }


    /**
     * 预约签到
     * @param request
     * @return
     */
    @PostMapping("/checkin")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:UPDATE"}, mode = SaMode.OR)
    public R appointmentCheckin(@RequestBody @Valid AppointmentCheckinRequest request) {
        AppointmentCheckinVO vo = misAppointmentService.appointmentCheckin(request);
        return R.OK().put("checkinVO", vo);
    }

    /**
     * 查询体检导引单摘要信息
     * @param appointmentId
     * @return
     */
    @GetMapping("/searchGuidanceInfo/{appointmentId}")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:SELECT"}, mode = SaMode.OR)
    public R searchGuidanceInfo(@PathVariable @Min(value = 1) Integer appointmentId) {
        GuidanceInfoVO guidanceInfoVO = misAppointmentService.searchGuidanceInfo(appointmentId);
        return R.OK().put("guidanceInfoVO", guidanceInfoVO);
    }

    /**
     * 修改预约的体检订单状态
     * @param request   订单的 uuid 和 status
     * @return
     */
    @PostMapping("/updateStatusByUuid")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:UPDATE"}, mode = SaMode.OR)
    public R updateAppointmentStatusByUuid(@RequestBody @Valid AppointmentStatusRequest request) {
        boolean bool = misAppointmentService.updateAppointmentStatusByUuid(request);
        return R.OK().put("result", bool);
    }

    /**
     * 医生检查页面客户摘要信息
     * @param uuid
     * @return
     */
    @GetMapping("/searchCustomerSummaryInfoByUuid/{uuid}")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:SELECT"}, mode = SaMode.OR)
    public R searchCustomerSummaryInfoByUuid(@PathVariable String uuid) {
        CustomerSummaryInfoVO summaryInfoVO = misAppointmentService.searchCustomerSummaryInfoByUuid(uuid);
        return R.OK().put("customerSummaryInfoVO", summaryInfoVO);
    }

    /**
     * 继续检查，获取当前体检的所有体检项的状态
     * @param uuid  体检单唯一编号
     * @return
     */
    @GetMapping("/continueCheckup/{uuid}")
    @SaCheckPermission(value = {"ROOT", "APPOINTMENT:SELECT", "APPOINTMENT:UPDATE"}, mode = SaMode.OR)
    public R continueCheckup(@PathVariable String uuid) {
        ContinueCheckupVO continueCheckupVO = misAppointmentService.continueCheckup(uuid);
        return R.OK().put("continueCheckupVO", continueCheckupVO);
    }
}
