package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leehuang.his.api.async.ReportAsync;
import com.leehuang.his.api.common.enums.ReportStatusEnum;
import com.leehuang.his.api.common.utils.MinioUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.dao.CheckupReportDao;
import com.leehuang.his.api.db.dao.CheckupResultDao;
import com.leehuang.his.api.db.entity.CheckupReportEntity;
import com.leehuang.his.api.db.pojo.CheckupResultEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.checkupReport.request.CheckupReportPageRequest;
import com.leehuang.his.api.mis.dto.checkupReport.vo.CheckupReportPageVO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import com.leehuang.his.api.mis.service.MisCheckupReportService;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisCheckupReportServicdeImpl extends ServiceImpl<CheckupReportDao, CheckupReportEntity> implements MisCheckupReportService {

    private final CheckupReportDao checkupReportDao;

    private final CheckupResultDao checkupResultDao;

    private final ReportAsync reportAsync;

    private final MinioUtil minioUtil;

    @Override
    public PageUtils<CheckupReportPageVO> searchCheckupReportByPage(CheckupReportPageRequest request) {
        // 1. 创建 Page 对象（当前页，每页大小）
        Page<CheckupReportPageVO> page = new Page<>(request.getPage(), request.getLength());

        // 2. 执行分页查询，MP 分页插件会拦截 selectByPage，检测是否存在 selectByPage_COUNT。如果存在，直接执行 selectByPage_COUNT 这个 SQL；如果不存在，才会自动生成 Count SQL
        IPage<CheckupReportPageVO> iPage = checkupReportDao.selectByPage(page, request);

        // 3. 对手机号脱敏，并计算年龄
        List<CheckupReportPageVO> records = iPage.getRecords();
        if (records != null && !records.isEmpty()) {
            records.forEach(vo -> {
                vo.setTel(DesensitizedUtil.mobilePhone(vo.getTel()));
                vo.setAge(Period.between(vo.getBirthday(), LocalDate.now()).getYears());
            });
        }

        // 4. 返回分页数据
        return new PageUtils<>(iPage.getTotal(), request.getLength(), request.getPage(), records);
    }

    /**
     * 异步生成体检报告
     * @param id    体检报告 id
     * @return      是否成功触发生成流程（已生成 / 正在生成：false，CAS 抢锁失败：false，真正开始 GENERATING：true）
     */
    @Override
    @Transactional
    public boolean createReport(Integer id, Integer generateType) {
        // 1. 查询并校验体检报告
        CheckupReportEntity reportEntity = checkupReportDao.selectById(id);
        if (reportEntity == null) {
            throw new HisException("体检报告记录不存在，ID=" + id);
        }

        if (!Objects.equals(reportEntity.getStatus(), ReportStatusEnum.NOT_GENERATED.getCode())) {
            log.debug("【体检报告生成】reportId={} 已生成或正在生成，直接返回", id);
            return false;
        }

        // 2. 查询并校验体检结果
        CheckupResultEntity resultEntity = checkupResultDao.searchById(reportEntity.getResultId());
        validateCheckupResult(resultEntity);

        // 3. 校验所有体检科室是否均已完成
        validateAllPlacesFinished(resultEntity);

        // 4. 更新体检报告状态为正在生成（使用 CAS 乐观锁）
        int rows = checkupReportDao.update(null, new LambdaUpdateWrapper<CheckupReportEntity>()
                .eq(CheckupReportEntity::getId, id)
                .eq(CheckupReportEntity::getStatus, ReportStatusEnum.NOT_GENERATED.getCode())
                .set(CheckupReportEntity::getStatus, ReportStatusEnum.GENERATING.getCode())
        );

        if (rows != 1) {
            log.info("【体检报告生成】reportId={} 已被其他请求处理或正在生成", id);
            // 返回 false 表示未实际触发生成
            return false;
        }

        // 5. 事务提交后异步执行生成任务
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("事务已提交，开始提交异步生成任务 ID={}", id);
                try {
                    reportAsync.generateReportAsync(id, generateType);
                } catch (Exception e) {
                    log.error("异步任务提交失败，尝试回滚状态 ID={}", id, e);
                    rollbackReportStatus(id, "异步任务提交失败：" + e.getMessage());
                }
            }
        });

        return true;
    }

    /**
     * 校验体检结果数据
     * @param resultEntity      mongoDB 中存储的体检结果对象
     */
    private void validateCheckupResult(CheckupResultEntity resultEntity) {
        if (resultEntity == null) {
            throw new HisException("体检结果数据不存在");
        }
        if (resultEntity.getCheckup() == null || resultEntity.getCheckup().isEmpty()) {
            throw new HisException("没有体检项目");
        }
        if (resultEntity.getResult() == null || resultEntity.getResult().isEmpty()) {
            throw new HisException("体检结果未录入");
        }
    }

    /**
     * 校验所有体检科室是否均已完成结果录入
     * @param resultEntity      mongoDB 中存储的体检结果对象
     */
    private void validateAllPlacesFinished(CheckupResultEntity resultEntity) {
        Set<String> placeSet = resultEntity.getCheckup().stream()
                .map(CheckupVO::getPlace)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> finishedPlaceSet = resultEntity.getResult().stream()
                .map(PlaceCheckupResultVO::getPlace)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        if (!finishedPlaceSet.containsAll(placeSet)) {
            Set<String> missingPlaces = new HashSet<>(placeSet);
            missingPlaces.removeAll(finishedPlaceSet);
            throw new HisException("存在未录入结果的体检科室：" + String.join(", ", missingPlaces));
        }
    }

    /**
     * 回滚体检报告状态
     * @param id        体检报告 id
     */
    private void rollbackReportStatus(Integer id, String errorMsg) {
        try {
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "未知错误";
            }
            if (errorMsg.length() > 450) {
                errorMsg = errorMsg.substring(0, 450);
            }

            // 回滚状态为 NOT_GENERATED，并记录错误信息
            int rows = checkupReportDao.update(null, new LambdaUpdateWrapper<CheckupReportEntity>()
                    .eq(CheckupReportEntity::getId, id)
                    .eq(CheckupReportEntity::getStatus, ReportStatusEnum.GENERATING.getCode())
                    .set(CheckupReportEntity::getStatus, ReportStatusEnum.NOT_GENERATED.getCode())
                    .set(CheckupReportEntity::getErrorMessage, errorMsg)
                    .set(CheckupReportEntity::getErrorTime, java.time.LocalDateTime.now())
            );

            if (rows == 1) {
                log.info("状态已回滚为 NOT_GENERATED ID={}", id);
            } else {
                log.warn("回滚状态时发现状态已变更，可能已被异步任务更新 ID={}", id);
            }
        } catch (Exception ex) {
            log.error("回滚报告状态失败，可能存在脏数据 ID={}，需要管理员检查数据", id, ex);
        }
    }

    /**
     * 后台系统下载体检报告
     * @param id
     * @param response
     */
    @Override
    public void downloadReport(Integer id, HttpServletResponse response) {
        // 1. 查询体检报告记录
        CheckupReportEntity reportEntity = checkupReportDao.selectById(id);

        if (reportEntity == null) {
            throw new HisException("体检报告记录不存在");
        }

        // 2.1 校验状态
        if (Objects.equals(reportEntity.getStatus(), ReportStatusEnum.NOT_GENERATED.getCode())
                || reportEntity.getFilePath() == null
                || reportEntity.getFilePath().trim().isEmpty()) {
            throw new HisException("报告不存在或未生成，无法下载");
        }

        if (Objects.equals(reportEntity.getStatus(), ReportStatusEnum.GENERATING.getCode())) {
            throw new HisException("报告正在生成中，请稍后再试");
        }

        // 2.2 只允许已生成 / 已邮寄状态下载（更严谨）
        if (!Objects.equals(reportEntity.getStatus(), ReportStatusEnum.GENERATED.getCode())
                && !Objects.equals(reportEntity.getStatus(), ReportStatusEnum.SENT.getCode())) {
            throw new HisException("当前报告状态不允许下载");
        }

        String filePath = reportEntity.getFilePath();

        // 3. 从 filePath 中提取文件名
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        if (fileName.trim().isEmpty()) {
            throw new HisException("报告文件名异常，无法下载");
        }

        // 4. 根据扩展名推断 contentType
        String contentType = minioUtil.getContentType(fileName);

        try {
            // 5.1 获取文件元信息（大小）
            StatObjectResponse stat = minioUtil.statFile(filePath);

            // 5.2 从 MinIO 下载并写入响应
            try (InputStream inputStream = minioUtil.downloadFile(filePath);
                 ServletOutputStream outputStream = response.getOutputStream()) {

                // 5.3 设置响应头
                response.setContentType(contentType);
                response.setCharacterEncoding("UTF-8");
                response.setContentLengthLong(stat.size());                     // 设置文件大小

                // 5.4 防止中文文件名乱码
                String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                        .replaceAll("\\+", "%20");

                response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
                response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

                // 5.5 文件流写出
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();

                log.info("体检报告下载成功，reportId={}, filePath={}", id, filePath);
            }
        } catch (Exception e) {
            log.error("下载体检报告失败，reportId={}, filePath={}", id, filePath, e);
            throw new HisException("下载体检报告失败");
        }
    }
}
