package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.IdcardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.enums.OrderStatusEnum;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.db.dao.AppointmentDao;
import com.leehuang.his.api.db.dao.OrderDao;
import com.leehuang.his.api.db.entity.AppointmentEntity;
import com.leehuang.his.api.db.entity.AppointmentRestrictionEntity;
import com.leehuang.his.api.db.entity.OrderEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentPageRequest;
import com.leehuang.his.api.front.dto.appointment.request.AppointmentRequest;
import com.leehuang.his.api.front.dto.appointment.vo.AppointmentPageVO;
import com.leehuang.his.api.front.service.AppointmentRestrictionService;
import com.leehuang.his.api.front.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service("appointmentService")
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private final StringRedisTemplate stringRedisTemplate;

    private final AppointmentDao appointmentDao;

    private final AppointmentRestrictionService restrictionService;

    private final OrderDao orderDao;

    private final MinioProperties minioProperties;

    /**
     * 插入新预约
     * @param request
     * @param customerId
     * @return
     */
    @Override
    @Transactional
    public String insertAppointment(AppointmentRequest request, Integer customerId) {

        // 校验请求参数
        validateAppointmentInfo(request, customerId);

        HashMap<String, String> resultCode = new HashMap<>() {{
            put("full", "当天预约已满，请选择其他日期");
            put("fail", "预约失败，请联系管理员");
            put("success", "预约成功");
        }};

        // 1. 从缓存中获取当天的预约数据，Redis 事务，判断当天是否可以预约体检
        String lua = " local key = KEYS[1] " +
                     " local real = tonumber(redis.call('HGET', key, 'realNum') or '0') " +          // 获取已预约人数
                     " local max = tonumber(redis.call('HGET', key, 'maxNum') or '0') " +            // 获取本日最大预约人数
                     " if real < max then " +                                                        // 若当天预约人数未满，则 +1
                     "   redis.call('HINCRBY', key, 'realNum', 1) " +
                     "   return 1 " +                                                                // 返回 1：成功
                     " else " +
                     "   return 0 " +                                                                // 返回 0：已满
                     " end ";

        Long luaResult = redisTemplate.execute(
                new DefaultRedisScript<>(lua, Long.class),
                Collections.singletonList("appointment#" + request.getAppointmentDate())
        );

        if (luaResult == null || luaResult == 0) {
            return resultCode.get("full");
        }

        // 2. 插入数据到 tb_appointment_restriction 表中，给预约人数 +1
        AppointmentRestrictionEntity entity = restrictionService.getBaseMapper()
                .selectOne(new LambdaQueryWrapper<AppointmentRestrictionEntity>()
                        .eq(AppointmentRestrictionEntity::getAppointmentDate, request.getAppointmentDate())
                );

        if (entity == null) {
            // 插入新数据，且设置 actual_appointment
            entity = new AppointmentRestrictionEntity();
            Integer maxNum = Integer.parseInt(Objects.requireNonNull(stringRedisTemplate.opsForValue().get("setting#appointment_number")));

            entity.setAppointmentDate(request.getAppointmentDate());
            entity.setActualAppointment(1);
            entity.setEverydayLimit(maxNum);
            entity.setActualLimit(maxNum);
            entity.setCreateTime(LocalDateTime.now());

            // 插入新数据到数据库
            restrictionService.save(entity);

        } else {
            // 往数据库中实际预约人员数 +1
            entity.setActualAppointment(entity.getActualAppointment() + 1);
            // 更新数据到数据库
            restrictionService.updateById(entity);
        }

        // 3. 将预约数据插入数据库
        AppointmentEntity appointmentEntity = new AppointmentEntity();
        BeanUtil.copyProperties(request, appointmentEntity);

        appointmentEntity.setUuid(IdUtil.simpleUUID().toUpperCase());
        appointmentEntity.setSex(IdcardUtil.getGenderByIdCard(request.getPid()) == 1 ? "男" : "女");
        appointmentEntity.setName(request.getName());
        String birthByIdCard = IdcardUtil.getBirthByIdCard(request.getPid());       // 转换时间类型
        appointmentEntity.setBirthday(LocalDate.parse(birthByIdCard, DateTimeFormatter.BASIC_ISO_DATE));
        appointmentEntity.setStatus(1);

        int rows = appointmentDao.insert(appointmentEntity);
        if (rows != 1) {
            return resultCode.get("fail");
        }

        // 4. 更新订单状态
        rows = orderDao.update(null,
                new LambdaUpdateWrapper<OrderEntity>()
                        .eq(OrderEntity::getId, request.getOrderId())
                        .eq(OrderEntity::getStatus, OrderStatusEnum.PAID.getCode())
                        .set(OrderEntity::getStatus, OrderStatusEnum.APPOINTED.getCode())
        );

        if (rows == 0) {
            return resultCode.get("fail");
        }

        // 5. 返回预约结果
        return resultCode.get("success");
    }

    /**
     * 分页查询用户体检数据
     * @param request
     * @param customerId
     * @return
     */
    @Override
    public PageUtils<AppointmentPageVO> searchAppointmentByPage(AppointmentPageRequest request, int customerId) {
        Integer page = request.getPage();
        Integer length = request.getLength();

        int start = (page - 1) * length;

        List<AppointmentPageVO> pageVOList = appointmentDao.searchAppointmentByPage(request, start, length, customerId);
        int totalCount = appointmentDao.searchAppointmentCountByPage(request, customerId);

        pageVOList.forEach(pageVO -> {
            if (pageVO.getFilePath() != null) {
                pageVO.setFilePath(minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + pageVO.getFilePath());
            }
        });

        return new PageUtils<>(totalCount, length, page, pageVOList);
    }

    /**
     * 校验请求数据
     * @param request
     * @param customerId
     */
    private void validateAppointmentInfo(AppointmentRequest request, Integer customerId ) {
        // 1. 校验预约客户是否拥有该订单
        Long count = orderDao.selectCount(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getId, request.getOrderId()).eq(OrderEntity::getCustomerId, customerId)
        );
        if (count != 1) {
            throw new HisException("预约失败，该订单不属于预约用户");
        }

        // 2. 校验身份证号
        if (!IdcardUtil.isValidCard18(request.getPid())) {
            throw new HisException("预约失败，身份证号无效");
        }

        // 3. 校验预约日期是否为 30 天内
        LocalDate appointmentDate = request.getAppointmentDate();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(30);

        // 4. TODO 校验当天该用户是否已预约该商品的体检项目

        boolean isIn = !appointmentDate.isBefore(startDate) && !appointmentDate.isAfter(endDate);

        if (!isIn) {
            throw new HisException("预约失败，可预约日期范围为明天起至" +
                    endDate + "（共30天）");
        }
    }
}
