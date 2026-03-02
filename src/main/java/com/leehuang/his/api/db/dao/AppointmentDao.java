package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.AppointmentEntity;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentPageRequest;
import com.leehuang.his.api.front.dto.appointment.vo.AppointmentPageVO;
import com.leehuang.his.api.mis.dto.appointment.dto.AppointmentSnapshotDTO;
import com.leehuang.his.api.mis.dto.appointment.request.MisAppointmentPageRequest;
import com.leehuang.his.api.mis.dto.appointment.vo.AppointmentVO;
import com.leehuang.his.api.mis.dto.appointment.dto.GuidanceSummaryInfoVO;
import com.leehuang.his.api.mis.dto.appointment.vo.CustomerSummaryInfoVO;
import com.leehuang.his.api.mis.dto.appointment.vo.OrderPageAppointmentVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_appointment(体检预约表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.AppointmentEntity
*/
public interface AppointmentDao extends BaseMapper<AppointmentEntity> {

    // mis 端 order 模块, 根据订单 id 获取预约信息
    List<OrderPageAppointmentVO> searchByOrderId(@Param("id") Integer id);

    // front 端分页查询用户预约信息
    List<AppointmentPageVO> searchAppointmentByPage(
            @Param("request") AppointmentPageRequest request,
            @Param("start") int start,
            @Param("length") Integer length,
            @Param("customerId") int customerId);

    // front 端分页查询用户体检信息总数
    int searchAppointmentCountByPage(@Param("request") AppointmentPageRequest request, @Param("customerId") int customerId);

    // mis 端分页查询用户预约信息
    List<AppointmentVO> searchAppointmentByPageForMis(
            @Param("request") MisAppointmentPageRequest request,
            @Param("start") int start,
            @Param("length") Integer length);

    // mis 端分页查询用户预约信息总数
    int searchAppointmentCountByPageForMis(MisAppointmentPageRequest request);

    // 批量软删除预约信息
    int deleteAppointmentByIds(Integer[] ids);

    // 获取体检快照信息
    AppointmentSnapshotDTO searchAppointmentSnapshotInfo(@Param("pid") String pid, @Param("currentDate") LocalDate now);

    // 获取体检人引导单摘要信息
    GuidanceSummaryInfoVO searchGuidanceSummaryInfo(@Param("appointmentId") Integer appointmentId);

    // 医生检查页面客户摘要信息
    CustomerSummaryInfoVO searchCustomerSummaryInfoByUuid(@Param("uuid") String uuid);
}




