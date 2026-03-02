package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.mis.dto.appointment.request.AppointmentCheckinRequest;
import com.leehuang.his.api.mis.dto.appointment.request.AppointmentStatusRequest;
import com.leehuang.his.api.mis.dto.appointment.request.MisAppointmentPageRequest;
import com.leehuang.his.api.mis.dto.appointment.request.MisHasAppointmentTodayRequest;
import com.leehuang.his.api.mis.dto.appointment.vo.*;
import com.leehuang.his.api.mis.dto.appointment.dto.GuidanceSummaryInfoVO;

import javax.validation.Valid;
import java.util.List;

public interface MisAppointmentService {
    // 根据订单 id 获取预约信息
    List<OrderPageAppointmentVO> searchByOrderId(Integer id);

    // mis 端分页查询获取预约信息
    PageUtils<AppointmentVO> searchAppointmentByPageForMis(MisAppointmentPageRequest request);

    // 批量删除预约信息
    int deleteAppointmentByIds(Integer[] ids);

    // 查询当前用户今日是否有预约
    int hasAppointInToday(MisHasAppointmentTodayRequest request);

    // 预约签到
    AppointmentCheckinVO appointmentCheckin(AppointmentCheckinRequest request);

    // 查询体检引导单摘要信息
    GuidanceInfoVO searchGuidanceInfo(Integer id);

    // 更新预约状态
    boolean updateAppointmentStatusByUuid(AppointmentStatusRequest request);

    // 医生检查页面客户摘要信息
    CustomerSummaryInfoVO searchCustomerSummaryInfoByUuid(String uuid);

    // 继续检查，获取当前体检的所有体检项的状态
    ContinueCheckupVO continueCheckup(String uuid);
}
