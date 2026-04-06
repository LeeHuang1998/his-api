package com.leehuang.his.api.mis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.CheckupReportEntity;
import com.leehuang.his.api.mis.dto.checkupReport.request.CheckupReportPageRequest;
import com.leehuang.his.api.mis.dto.checkupReport.vo.CheckupReportPageVO;

import javax.servlet.http.HttpServletResponse;

public interface MisCheckupReportService extends IService<CheckupReportEntity> {

    // 分页查询体检报告数据
    PageUtils<CheckupReportPageVO> searchCheckupReportByPage(CheckupReportPageRequest request);

    // 生成体检报告
    boolean createReport(Integer id, Integer generateType);

    // 下载体检报告
    void downloadReport(Integer id, HttpServletResponse response);
}
