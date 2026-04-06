package com.leehuang.his.api.job;

import com.leehuang.his.api.common.enums.AppointmentStatusEnum;
import com.leehuang.his.api.common.enums.ReportStatusEnum;
import com.leehuang.his.api.db.dao.CheckupReportDao;
import com.leehuang.his.api.db.entity.CheckupReportEntity;
import com.leehuang.his.api.mis.service.MisCheckupReportService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 体检报告自动生成定时任务
 * 每天凌晨 1 点执行，检查并生成逾期未生成的报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckupReportJobHandler {

    private final MisCheckupReportService checkupReportService;

    private final CheckupReportDao checkupReportDao;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 定时任务入口
     * 执行时间：每天凌晨 1:00
     * 任务逻辑：检查三天前的凌晨 1 点 到今天凌晨 1 点前所有状态为 NOT_GENERATED 且预约已完成的报告并生成
     */
    @XxlJob("checkupReportAutoGenerateJob")
    public ReturnT<String> autoGenerateReports() {
        log.info("========== 开始执行体检报告自动生成任务 ==========");

        LocalDateTime jobStartTime = LocalDateTime.now();
        AtomicInteger triggerCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        try {
            // 1. 计算时间范围（三天前的凌晨 1 点 到 今天的凌晨 1 点）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.with(LocalTime.of(1, 0));       // 今天凌晨 1:00
            LocalDateTime startTime = endTime.minusDays(3);                          // 三天前的凌晨 1:00

            log.info("检查时间范围：{} 至 {}", startTime.format(FORMATTER), endTime.format(FORMATTER));

            // 2. 查询符合条件的体检报告（三天内体检已完成且未生成报告的记录）
            List<CheckupReportEntity> reportsToGenerate = findReportsToGenerate(startTime, endTime, 600);

            if (reportsToGenerate.isEmpty()) {
                log.info("没有找到需要生成的报告");
                return ReturnT.SUCCESS;
            }

            log.info("找到 {} 份需要生成的报告", reportsToGenerate.size());

            // 3. 逐个生成报告
            for (CheckupReportEntity report : reportsToGenerate) {
                try {
                    log.info("开始生成报告 ID={}, appointmentId={}, date={}",
                            report.getId(), report.getAppointmentId(), report.getDate());

                    // 异步生成报告（返回 true 时为本线程生成报告，返回 false 时为被其他线程处理）
                    boolean triggered = checkupReportService.createReport(report.getId(), 2);
                    if (triggered) {
                        triggerCount.incrementAndGet();
                    } else {
                        skipCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    log.error("生成报告失败 ID={}", report.getId(), e);
                    failCount.incrementAndGet();
                }
            }

            Duration duration = Duration.between(jobStartTime, now);
            log.info("========== 体检报告生成任务完成 ========== 总扫描：{}, 实际触发：{}, 跳过：{}, 失败：{}, 耗时：{} 秒",
                    reportsToGenerate.size(),
                    triggerCount.get(),
                    skipCount.get(),
                    failCount.get(),
                    duration.getSeconds()
            );

            return failCount.get() == 0 ? ReturnT.SUCCESS : ReturnT.FAIL;

        } catch (Exception e) {
            log.error("体检报告自动生成任务执行异常", e);
            return ReturnT.FAIL;
        }
    }

    /**
     * 查询需要生成的报告
     * 条件：
     * 1. 报告状态为 NOT_GENERATED
     * 2. 报告日期在指定时间范围内
     * 3. 对应的预约状态为已完成 (status=3)
     */
    private List<CheckupReportEntity> findReportsToGenerate(LocalDateTime startDateTime, LocalDateTime endDateTime, Integer limit) {
        // 1. 获取三天前到今天，且状态为已完成的预约体检
        List<CheckupReportEntity> entityList = checkupReportDao.selectPendingAutoGenerateReports(
                ReportStatusEnum.NOT_GENERATED.getCode(),
                AppointmentStatusEnum.CHECK_COMPLETED.getCode(),
                startDateTime,
                endDateTime,
                limit
        );

        log.debug("查询到 {} 条待生成报告", entityList.size());

        return entityList;
    }
}
