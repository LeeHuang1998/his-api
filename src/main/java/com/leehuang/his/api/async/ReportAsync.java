package com.leehuang.his.api.async;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehuang.his.api.common.enums.ReportStatusEnum;
import com.leehuang.his.api.common.utils.CheckupReportUtil;
import com.leehuang.his.api.common.utils.MinioUtil;
import com.leehuang.his.api.db.dao.AppointmentDao;
import com.leehuang.his.api.db.dao.CheckupReportDao;
import com.leehuang.his.api.db.dao.CheckupResultDao;
import com.leehuang.his.api.db.entity.CheckupReportEntity;
import com.leehuang.his.api.db.pojo.CheckupResultEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.report.CheckupItemDTO;
import com.leehuang.his.api.mis.dto.report.CheckupReportDTO;
import com.leehuang.his.api.mis.dto.report.CheckupResultDTO;
import com.leehuang.his.api.mis.mapper.CheckupReportConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportAsync {

    private final CheckupReportDao checkupReportDao;

    private final CheckupResultDao checkupResultDao;

    private final AppointmentDao appointmentDao;

    private final CheckupReportConverter checkupReportConverter;

    private final CheckupReportUtil checkupReportUtil;

    private final MinioUtil minioUtil;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 异步生成体检报告，使用 AsyncTaskExecutor 线程池异步执行
     * @param id 体检报告 ID
     */
    @Async("AsyncTaskExecutor")
    public void generateReportAsync(Integer id, Integer generateType) {
        String filePath = null;
        boolean uploadSuccess = false;

        try {
            // 1. 从数据库中查询体检报告数据
            CheckupReportEntity reportEntity = checkupReportDao.selectById(id);
            if (reportEntity == null) {
                log.error("异步生成失败：报告不存在 ID={}", id);
                throw new HisException("体检报告不存在");
            }

            // 2. 查询体检结果
            CheckupResultEntity resultEntity = checkupResultDao.searchById(reportEntity.getResultId());
            if (resultEntity == null) {
                log.error("数据异常：体检结果不存在 ID={}, resultId={}", id, reportEntity.getResultId());
                throw new HisException("数据异常：体检结果不存在");
            }

            // 3. 查询生成报告所需要的数据
            CheckupReportDTO dto = appointmentDao.searchDataForReport(reportEntity.getAppointmentId());
            if (dto == null) {
                log.error("预约数据不存在 ID={}, appointmentId={}", id, reportEntity.getAppointmentId());
                throw new HisException("预约数据不存在");
            }

            if (dto.getBirthday() == null) {
                log.warn("体检人生日为空 ID={}, 使用默认年龄 0", id);
                dto.setAge(0);
            } else {
                int age = Period.between(dto.getBirthday(), reportEntity.getDate()).getYears();
                // 边界检查：年龄不能为负数或超过合理范围
                if (age < 0 || age > 150) {
                    log.warn("体检人年龄异常 ID={}, birthday={}, 计算年龄={}", id, dto.getBirthday(), age);
                    age = 0;
                }
                dto.setAge(age);
            }

            // 设置体检报告中的所有检查项和检查结果
            // 检查项
            List<CheckupItemDTO> checkupItemDTOList = resultEntity.getCheckup().stream()
                    .filter(Objects::nonNull)
                    .map(checkup -> {
                        CheckupItemDTO itemDTO = new CheckupItemDTO();
                        itemDTO.setPlace(checkup.getPlace() != null ? checkup.getPlace() : "未知科室");
                        itemDTO.setItem(checkup.getItem() != null ? checkup.getItem() : "未知项目");
                        return itemDTO;
                    })
                    .collect(Collectors.toList());

            // 检查结果
            List<CheckupResultDTO> checkupResultDTOList =
                    checkupReportConverter.toCheckupResultDTOList(resultEntity.getResult());

            dto.setCheckup(checkupItemDTOList);
            dto.setResult(checkupResultDTOList);

            // 4. 按照体检日期生成分层路径
            LocalDate reportDate = reportEntity.getDate();
            filePath = String.format("/report/checkup/%d/%02d/%02d/%s.docx",
                    reportDate.getYear(),
                    reportDate.getMonthValue(),
                    reportDate.getDayOfMonth(),
                    reportEntity.getResultId()
            );

            // 5. 生成 Word 文档
            try (XWPFDocument report = checkupReportUtil.createReport(dto)) {

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                report.write(out);
                byte[] reportBytes = out.toByteArray();

                try (InputStream in = new ByteArrayInputStream(reportBytes)) {
                    minioUtil.uploadWord(filePath, in, reportBytes.length);
                    uploadSuccess = true;
                }
            }

            // 6. 更新体检报告状态，成功后清空错误信息
            int rows = checkupReportDao.update(null, new LambdaUpdateWrapper<CheckupReportEntity>()
                    .eq(CheckupReportEntity::getId, id)
                    .eq(CheckupReportEntity::getStatus, ReportStatusEnum.GENERATING.getCode())
                    .set(CheckupReportEntity::getStatus, ReportStatusEnum.GENERATED.getCode())
                    .set(CheckupReportEntity::getFilePath, filePath)
                    .set(CheckupReportEntity::getGeneratedTime, LocalDateTime.now())
                    .set(CheckupReportEntity::getGenerateType, generateType)
                    .set(CheckupReportEntity::getErrorMessage, null)
                    .set(CheckupReportEntity::getErrorTime, null)
            );

            if (rows != 1) {
                log.error("更新报告状态失败，ID={}，当前体检报告数据状态可能已变更", id);
                throw new HisException("更新报告状态失败");
            }

            log.info("【体检报告生成完成】reportId={}, filePath={}", id, filePath);

            // 7. 发送 WebSocket 广播通知
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("reportId", id);
                payload.put("status", ReportStatusEnum.GENERATED.getCode());
                // 广播给所有订阅了 /topic/checkup-report 的后台用户
                messagingTemplate.convertAndSend("/topic/checkup-report", payload);
                log.info("【WebSocket 推送成功】报告生成成功，发送通知, reportId={}", id);
            } catch (Exception ex) {
                log.error("【WebSocket 推送失败】reportId={}", id, ex);
                // 推送失败不影响主流程，只记日志
            }


        } catch (Exception e) {
            log.error("生成体检报告失败 reportId={}", id, e);

            // 若上传成功，则删除生成的文件
            if (uploadSuccess) {
                try {
                    minioUtil.removeFile(filePath);
                    log.info("已清理生成的文件 filePath={}", filePath);
                } catch (Exception ex) {
                    log.error("清理文件失败 filePath={}", filePath, ex);
                }
            }

            // 提取错误信息
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.length() > 450) {
                errorMsg = errorMsg == null ? "未知错误" : errorMsg.substring(0, 450);
            }

            // 回滚状态并检查返回值，记录错误信息
            int rollbackRows = checkupReportDao.update(null, new LambdaUpdateWrapper<CheckupReportEntity>()
                    .eq(CheckupReportEntity::getId, id)
                    .eq(CheckupReportEntity::getStatus, ReportStatusEnum.GENERATING.getCode())
                    .set(CheckupReportEntity::getStatus, ReportStatusEnum.NOT_GENERATED.getCode())
                    .set(CheckupReportEntity::getErrorMessage, errorMsg)
                    .set(CheckupReportEntity::getErrorTime, LocalDateTime.now())
            );

            if (rollbackRows != 1) {
                log.error("回滚报告状态失败，可能存在脏数据 ID={}, 当前状态可能已变更", id);
                throw new HisException("回滚报告状态失败，请联系管理员检查数据", e);
            } else {
                log.info("状态已回滚为 NOT_GENERATED ID={}, errorMessage={}", id, errorMsg);
            }
        }
    }
}
