package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leehuang.his.api.db.entity.CheckupReportEntity;
import com.leehuang.his.api.mis.dto.checkupReport.request.CheckupReportPageRequest;
import com.leehuang.his.api.mis.dto.checkupReport.vo.CheckupReportPageVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_checkup_report(体检报告表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.CheckupReportEntity
*/
public interface CheckupReportDao extends BaseMapper<CheckupReportEntity> {

    // 插入体检结果记录到体检报告表中
    int insert(@Param("uuid") String uuid, @Param("resultId") String resultId);

    // 分页查询体检报告
    IPage<CheckupReportPageVO> selectByPage(Page<CheckupReportPageVO> page, @Param("request") CheckupReportPageRequest request);

    // 查询待自动生成的报告
    List<CheckupReportEntity> selectPendingAutoGenerateReports(
            @Param("reportStatus") Integer reportStatus,
            @Param("appointmentStatus") Integer appointmentStatus,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("limit") Integer limit);

}

