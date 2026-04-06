package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.mis.dto.checkupReport.request.CheckupReportPageRequest;
import com.leehuang.his.api.mis.dto.checkupReport.vo.CheckupReportPageVO;
import com.leehuang.his.api.mis.service.MisCheckupReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequestMapping("/mis/checkup_report")
@RequiredArgsConstructor
public class MisCheckupReportController {

    private final MisCheckupReportService misCheckupReportService;

    /**
     * 分页查询体检报告数据
     * @param request
     * @return
     */
    @PostMapping("/searchCheckupReportByPage")
    @SaCheckPermission(value = {"ROOT", "CHECKUP_REPORT:SELECT"}, mode = SaMode.OR)
    public R searchCheckupReportByPage(@Valid @RequestBody CheckupReportPageRequest request) {
        PageUtils<CheckupReportPageVO> pageData = misCheckupReportService.searchCheckupReportByPage(request);
        return R.OK().put("pageData", pageData);
    }

    /**
     * 生成体检报告
     * @param request       获取体检报告 id
     * @return              生成结果
     */
    @PostMapping("/createReport")
    @SaCheckPermission(value = {"ROOT", "CHECKUP_REPORT:SELECT"}, mode = SaMode.OR)
    public R createReport(@RequestBody @Valid IdRequest request) {
        // 第二个参数为生成报告类型，1为手动，2为自动
        boolean bool = misCheckupReportService.createReport(request.getId(), 1);
        return R.OK().put("result", bool);
    }

    /**
     * 下载体检报告
     * @param id        体检报告ID
     * @param response  HTTP 响应对象
     */
    @GetMapping("/downloadReport/{id}")
    @SaCheckPermission(value = {"ROOT", "CHECKUP_REPORT:SELECT"}, mode = SaMode.OR)
    public void downloadReport(@PathVariable Integer id, HttpServletResponse response) {
        misCheckupReportService.downloadReport(id, response);
    }
}
