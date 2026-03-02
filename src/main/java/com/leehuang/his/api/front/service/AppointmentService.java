package com.leehuang.his.api.front.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentPageRequest;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentRequest;
import com.leehuang.his.api.front.dto.appointment.vo.AppointmentPageVO;

public interface AppointmentService {
    // 插入新预约
    String insertAppointment(AppointmentRequest request, Integer customerId);

    // front 分页查询预约
    PageUtils<AppointmentPageVO> searchAppointmentByPage(AppointmentPageRequest request, int customerId);
}
