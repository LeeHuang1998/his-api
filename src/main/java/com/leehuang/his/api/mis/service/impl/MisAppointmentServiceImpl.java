package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.util.IdcardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehuang.his.api.common.enums.OrderStatusEnum;
import com.leehuang.his.api.common.utils.FaceIAIUtil;
import com.leehuang.his.api.common.utils.MinioUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.enums.AppointmentStatusEnum;
import com.leehuang.his.api.common.utils.QrCodeUtil;
import com.leehuang.his.api.db.dao.*;
import com.leehuang.his.api.db.entity.AppointmentEntity;
import com.leehuang.his.api.db.entity.FlowRegulationEntity;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.db.pojo.CheckupResultEntity;
import com.leehuang.his.api.db.pojo.GoodsSnapshotEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.appointment.dto.AppointmentSnapshotDTO;
import com.leehuang.his.api.mis.dto.appointment.request.AppointmentCheckinRequest;
import com.leehuang.his.api.mis.dto.appointment.request.AppointmentStatusRequest;
import com.leehuang.his.api.mis.dto.appointment.request.MisAppointmentPageRequest;
import com.leehuang.his.api.mis.dto.appointment.request.MisHasAppointmentTodayRequest;
import com.leehuang.his.api.mis.dto.appointment.vo.*;
import com.leehuang.his.api.mis.dto.appointment.dto.GuidanceSummaryInfoVO;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.CheckupProgressDTO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import com.leehuang.his.api.mis.dto.order.dto.OrderAppointmentFinishedDTO;
import com.leehuang.his.api.mis.service.MisAppointmentService;
import com.leehuang.his.api.mis.service.MisFlowRegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MisAppointmentServiceImpl implements MisAppointmentService {

    private final AppointmentDao appointmentDao;

    private final FaceIAIUtil faceIAIUtil;

    private final MinioUtil minioUtil;

    private final GoodsSnapshotDao goodsSnapshotDao;

    private final CheckupResultDao checkupResultDao;

    private final OrderDao orderDao;

    private final CheckupReportDao checkupReportDao;

    private final MisFlowRegulationService misFlowRegulationService;

    /**
     * 根据订单 id 获取预约信息
     * @param id
     * @return
     */
    @Override
    public List<OrderPageAppointmentVO> searchByOrderId(Integer id) {
        return appointmentDao.searchByOrderId(id);
    }

    /**
     * mis 端分页查询预约数据
     * @param request
     * @return
     */
    @Override
    public PageUtils<AppointmentVO> searchAppointmentByPageForMis(MisAppointmentPageRequest request) {
        Integer page = request.getPage();
        Integer length = request.getLength();

        int start = (page - 1) * length;

        if(request.getAppointmentDate() == null) {
            request.setAppointmentDate(LocalDate.now());
        }

        List<AppointmentVO> appointmentVOList = appointmentDao.searchAppointmentByPageForMis(request, start, length);
        int totalCount = appointmentDao.searchAppointmentCountByPageForMis(request);

        return new PageUtils<>(totalCount, length, page, appointmentVOList);
    }

    /**
     * 批量删除预约信息
     * @param ids
     * @return
     */
    @Override
    public int deleteAppointmentByIds(Integer[] ids) {
        return appointmentDao.deleteAppointmentByIds(ids);
    }

    /**
     * 判断该用户是否今日有预约
     * @param request
     * @return
     */
    @Override
    public int hasAppointInToday(MisHasAppointmentTodayRequest request) {

        // 构建查询条件
        LambdaQueryWrapper<AppointmentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppointmentEntity::getPid, request.getPid())
                .eq(AppointmentEntity::getName, request.getName())
                .eq(AppointmentEntity::getSex, request.getSex())
                .eq(AppointmentEntity::getAppointmentDate, LocalDate.now())
                .eq(AppointmentEntity::getIsDeleted, 0);                    // 逻辑删除

        // MyBatisPlus 的 select 查询字段方法使用了可变参数（varargs）和泛型，而 Java 在处理泛型数组时会产生 unchecked 警告
        // 使用 @SuppressWarnings("unchecked") 忽略警告
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<AppointmentEntity> finalWrapper =
                wrapper.select(AppointmentEntity::getId, AppointmentEntity::getStatus)
                       .last("LIMIT 1");
        // 查询结果
        AppointmentEntity appointmentEntity = appointmentDao.selectOne(finalWrapper);
        System.out.println(appointmentEntity);
        if (appointmentEntity == null) {
            return AppointmentStatusEnum.NULL_APPOINTMENT.getCode();
        } else {
            return AppointmentStatusEnum.fromCode(appointmentEntity.getStatus()).getCode();
        }
    }

    /**
     * 预约签到
     * 签到流程：刷身份证 -> 拍摄现场照片 -> 人脸识别 -> 验证通过后，上传拍摄图 -> 更新数据库签到状态
     *          -> 筛选出商品中适合体检人性别的体检项目 -> 将体检项目数据插入到 mongodb 中（用于之后保存体检结果数据）
     * @param request
     * @return
     */
    @Override
    @Transactional
    public AppointmentCheckinVO appointmentCheckin(AppointmentCheckinRequest request) {

        String personName = request.getName();
        String pid = request.getPid();
        String idCardImage = request.getPhoto_1();
        String cameraPhoto = request.getPhoto_2();
        Long gender = IdcardUtil.getGenderByIdCard(pid) == 1 ? 1L : 2L;
        String sex = IdcardUtil.getGenderByIdCard(pid) == 1 ? "男" : "女";

        // 判断当前客户今日是否已经签到过
        AppointmentEntity appointmentEntity = appointmentDao.selectOne(new LambdaQueryWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getPid, pid)
                .eq(AppointmentEntity::getAppointmentDate, LocalDate.now())
                .isNotNull(AppointmentEntity::getCheckinTime)
        );

        if (appointmentEntity == null) {
            throw new HisException("没有找到客户预约数据，请确认该客户是否预约");
        } else if (Objects.equals(appointmentEntity.getStatus(), AppointmentStatusEnum.CHECK_IN.getCode())) {
            throw new HisException("该客户已经签到，请勿重复签到");
        } else if (Objects.equals(appointmentEntity.getStatus(), AppointmentStatusEnum.CHECK_COMPLETED.getCode())) {
            throw new HisException("该客户已经体检完毕，请勿重复签到");
        } else if (Objects.equals(appointmentEntity.getStatus(), AppointmentStatusEnum.CHECK_CLOSED.getCode())) {
            throw new HisException("该客户体检已关闭，请勿签到");
        }

        String uuid = appointmentEntity.getUuid();

        // 1. 传入身份证照和拍摄图片到腾讯云人脸识别接口，执行人脸对比 + 静态活体识别
        boolean verifyResult = faceIAIUtil.verifyFaceModel(personName, pid, gender, idCardImage, cameraPhoto);

        // 2. 人脸识别成功，更新数据库信息
        if (verifyResult) {
            // 2.1 上传图片到 minio 中
            // 根据传递的 cameraPhoto 获取图片的 base64 字符串，MIME 值和图片扩展类型
            // 第一个元素为纯 Base64 字符串，第二个元素为 MIME 类型，第三个元素为文件扩展名
            String[] mimeTypeAndExtension = minioUtil.getMimeTypeAndExtension(cameraPhoto);

            // 设置上传路径和文件名
            String path = "mis/appointment/checkin/"
                    + LocalDate.now() + "/"
                    + UUID.randomUUID().toString().replace("-", "")
                    + mimeTypeAndExtension[2];
            // 将签到照片保存到 minio 中
            minioUtil.uploadBase64Image(path, mimeTypeAndExtension[0], mimeTypeAndExtension[1]);

            // 2.2 更新数据库信息，修改签到状态为已签到
            LambdaUpdateWrapper<AppointmentEntity> updateWrapper =
                    new LambdaUpdateWrapper<AppointmentEntity>()
                            .eq(AppointmentEntity::getPid, pid)
                            .eq(AppointmentEntity::getStatus, AppointmentStatusEnum.NOT_CHECK_IN.getCode())
                            .eq(AppointmentEntity::getAppointmentDate, LocalDate.now())
                            .set(AppointmentEntity::getStatus, AppointmentStatusEnum.CHECK_IN.getCode())
                            .set(AppointmentEntity::getCheckinTime, LocalDateTime.now());
            int updateRows = appointmentDao.update(null, updateWrapper);

            if (updateRows != 1) {
                log.error("预约单号：{}，用户：[{},{}]更新签到状态失败", uuid, personName, LocalDateTime.now());
                throw new HisException("更新签到状态失败");
            }

            // 3. 获取体检流水号和商品快照 id
            AppointmentSnapshotDTO snapshotDTO =  appointmentDao.searchAppointmentSnapshotInfo(pid, LocalDate.now());
            // 3.1 筛选适合体检人性别的体检项目
            GoodsSnapshotEntity goodsSnapshotById = goodsSnapshotDao.getGoodsSnapshotById(snapshotDTO.getSnapshotId());

            List<CheckupVO> checkupVOList = goodsSnapshotById.getCheckup().stream()
                    .filter(checkup -> "无".equals(checkup.getSex()) || Objects.equals(checkup.getSex(), sex))
                    .collect(Collectors.toList());

            // 3.2 生成体检结果快照
            String _id = checkupResultDao.insert(snapshotDTO.getUuid(), personName, checkupVOList);
            if (_id == null || _id.isBlank()) {
                log.error("体检单 uuid：【{}】，生成体检结果快照失败", uuid);
                throw new HisException("生成体检结果快照失败");
            }

            // 3.3 插入数据到 tb_checkup_report 表中
            int insert = checkupReportDao.insert(uuid, _id);
            if (insert != 1) {
                log.error("体检单 uuid：【{}】，体检结果报告插入失败", uuid);
                throw new HisException("体检单 uuid：【" + uuid + "】，体检结果报告插入失败");
            }

            // 4. 获取当前推荐的科室，签到时肯定没有做过任何检查，所以下一个科室需要排队，传入 true
            NextPlaceVO dto = misFlowRegulationService.recommendNextPlace(snapshotDTO.getUuid(), true);

            AppointmentCheckinVO checkinVO = new AppointmentCheckinVO();
            checkinVO.setCheckinResult(true);
            checkinVO.setNextPlaceVO(dto);

            // 5. 返回签到结果
            return checkinVO;
        } else {
            log.error("用户：[{},{}]签到失败", personName, LocalDateTime.now());
            throw new HisException("签到失败，未通过人脸识别");
        }
    }

    /**
     * 查询体检引导单摘要信息
     * @param id
     * @return
     */
    @Override
    public GuidanceInfoVO searchGuidanceInfo(Integer id) {
        // 1. 获取摘要信息
        GuidanceSummaryInfoVO summaryInfoVO = appointmentDao.searchGuidanceSummaryInfo(id);
        // 2. 计算年龄
        summaryInfoVO.setAge(Period.between(summaryInfoVO.getBirthday(), LocalDate.now()).getYears());
        // 3. 生成二维码 base64
        String qrcodeBase64 = QrCodeUtil.generateBase64WithPrefix(summaryInfoVO.getUuid(), 150, 150);
        // 4. 筛选适合体检人的体检项目并去重
        GoodsSnapshotEntity goodsSnapshotById = goodsSnapshotDao.getGoodsSnapshotById(summaryInfoVO.getSnapshotId());
        // 4.1 只保留 name 和 place 属性，并去重
        // 去重：在 checkupVO 中，重写了 hashcode 和 equals 方法，当 name（体检项目名） 和 place（体检地点） 相同时视为同一对象，LinkHashSet 会根据这两个方法自动去重
        LinkedHashSet<CheckupVO> checkupSet = goodsSnapshotById.getCheckup().stream()
                .filter(checkup -> "无".equals(checkup.getSex()) || Objects.equals(checkup.getSex(), summaryInfoVO.getSex()))
                .map(checkupVO -> CheckupVO.of(checkupVO.getName(), checkupVO.getPlace()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        System.out.println(checkupSet.size());

        // 5. 返回结果
        GuidanceInfoVO guidanceInfoVO = new GuidanceInfoVO();
        guidanceInfoVO.setSummaryInfo(summaryInfoVO);
        guidanceInfoVO.setQrcodeBase64(qrcodeBase64);
        guidanceInfoVO.setCheckup(checkupSet);

        return guidanceInfoVO;
    }

    /**
     * 修改体检预约状态
     * @param request
     * @return
     */
    @Override
    @Transactional
    public boolean updateAppointmentStatusByUuid(AppointmentStatusRequest request) {

        // 1. 判断本次体检是否所有项目都已完成
        CheckupProgressDTO progress = checkupResultDao.getCheckupProgress(request.getUuid());

        if (!progress.isAllFinished()) {
            throw new HisException("本次体检预约尚未完成所有体检项目");
        }

        // 2. 更新体检预约状态（ status 不为 CHECK_FINISHED 的预约记录）
        LambdaUpdateWrapper<AppointmentEntity> updateWrapper = new LambdaUpdateWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getUuid, request.getUuid())
                .eq(AppointmentEntity::getStatus, AppointmentStatusEnum.CHECK_IN.getCode())
                .set(AppointmentEntity::getStatus, AppointmentStatusEnum.CHECK_COMPLETED.getCode())
                .set(AppointmentEntity::getCompletedTime, LocalDateTime.now());

        int rows = appointmentDao.update(null, updateWrapper);
        if (rows != 1) {
            throw new HisException("更新预约状态失败");
        }

        // 3. 该体检的状态更新完成后，判断当前订单的所有商品是否都已完成预约
        OrderAppointmentFinishedDTO dto = orderDao.searchOrderFinished(request.getUuid());
        // 若订单的商品数与预约完成数相同，则更新订单状态为已结束，否则不修改订单状态，因为在 insertAppointment 中，在预约数小于订单商品数时已经修改过订单状态了
        if (Objects.equals(dto.getGoodsCount(), dto.getAppointmentFinishedCount())) {
            LambdaUpdateWrapper<OrderEntity> updateStatusWrapper = new LambdaUpdateWrapper<OrderEntity>()
                    .eq(OrderEntity::getId, dto.getOrderId())
                    .eq(OrderEntity::getStatus,OrderStatusEnum.ALLAPPOINTED)
                    .set(OrderEntity::getStatus, OrderStatusEnum.COMPLETED.getCode());
            rows = orderDao.update(null, updateStatusWrapper);
            if (rows != 1) {
                log.error("订单中所有预约都已完成，但订单状态更新为 ALLAPPOINTED 失败");
                throw new HisException("更新订单状态失败");
            }
        }

        return true;
    }

    /**
     * 医生检查页面客户摘要信息
     * @param uuid
     * @return
     */
    @Override
    public CustomerSummaryInfoVO searchCustomerSummaryInfoByUuid(String uuid) {
        // 1. 获取摘要信息
        CustomerSummaryInfoVO summaryInfoVO = appointmentDao.searchCustomerSummaryInfoByUuid(uuid);

        if (summaryInfoVO == null) {
            throw new HisException("未查询到体检预约信息");
        }
        if (Objects.equals(summaryInfoVO.getStatus(), AppointmentStatusEnum.NOT_CHECK_IN.getCode())) {
            throw new HisException("该预约尚未签到");
        }
        if (Objects.equals(summaryInfoVO.getStatus(), AppointmentStatusEnum.CHECK_COMPLETED.getCode())) {
            throw new HisException("该预约已结束");
        }

        // 2. 计算年龄
        summaryInfoVO.setAge(Period.between(summaryInfoVO.getBirthday(), LocalDate.now()).getYears());

        return summaryInfoVO;
    }

    /**
     * 继续检查，获取当前体检的所有体检项的状态
     * @param uuid
     * @return
     */
    @Override
    public ContinueCheckupVO continueCheckup(String uuid) {
        ContinueCheckupVO continueCheckupVO = new ContinueCheckupVO();

        // 1. 获取客户摘要信息
        CustomerSummaryInfoVO summaryInfoVO = appointmentDao.searchCustomerSummaryInfoByUuid(uuid);

        if (summaryInfoVO == null) {
            throw new HisException("没有找到体检签到信息");
        }

        // 2. 计算年龄
        summaryInfoVO.setAge(Period.between(summaryInfoVO.getBirthday(), LocalDate.now()).getYears());

        // basicInfo 设置到 continueCheckupVO 中
        continueCheckupVO.setSummaryInfoVO(summaryInfoVO);

        // 3. mongoDB 中获取所有体检项和已完成体检项
        CheckupResultEntity entity = checkupResultDao.searchResultByUuid(uuid);

        // 4. 筛选出已完成的科室和所有科室，并判断是否已完成
        if (entity == null || entity.getCheckup() == null) {
            throw new HisException("没有找到体检检查内容，请联系管理员");
        }

        // 4.1 所有科室
        Set<String> allPlaceSet = entity.getCheckup().stream().map(CheckupVO::getPlace).map(String::trim).collect(Collectors.toSet());

        // 查询这些 place 的 is_delete 状态是否为 0
        Map<String, Integer> placeStatusMap = misFlowRegulationService.getBaseMapper()
                .selectList(new LambdaQueryWrapper<FlowRegulationEntity>().in(FlowRegulationEntity::getPlace, allPlaceSet))
                .stream().collect(Collectors.toMap(FlowRegulationEntity::getPlace, FlowRegulationEntity::getIsDeleted));

        // 4.2 已完成科室，若 finishedPlace 为空则返回空 List
        Set<String> finishedPlace = Optional.ofNullable(entity.getResult())
                .orElse(Collections.emptyList())
                .stream()
                .map(PlaceCheckupResultVO::getPlace)
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toSet());

        // 4.3 判断每个科室是否已完成体检（筛选掉逻辑删除的数据，并给停用的科室设置为已完成）
        List<CheckupCompletedInfoVO> allPlaceIsCompleted = allPlaceSet.stream().filter(place -> placeStatusMap.get(place) != 1).map(place -> {
            CheckupCompletedInfoVO vo = new CheckupCompletedInfoVO();
            Integer status = placeStatusMap.get(place);
            if (status == 2) {
                vo.setPlace(place + "（暂时停用）");
                vo.setIsCompleted(null);
            } else {
                vo.setPlace(place);
                vo.setIsCompleted(finishedPlace.contains(place));
            }
            return vo;
        }).collect(Collectors.toList());

        // 5. 设置结果
        continueCheckupVO.setCheckupPlace(allPlaceIsCompleted);

        return continueCheckupVO;
    }
}
