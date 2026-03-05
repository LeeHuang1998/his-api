package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.IdcardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehuang.his.api.common.enums.CheckInStatusEnum;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service("appointmentService")
@Slf4j
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

        // 1.1 Redis 防抖，防止用户重复点击预约
        // 构建防重复点击key
        String repeatKey = "appointment:repeat:" + request.getOrderId();

        // SETNX 方式设置锁（5 秒防重复点击）
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(repeatKey, "1", 5, TimeUnit.SECONDS);

        // 若锁获取失败则说明用户短时间内重复点击
        if (Boolean.FALSE.equals(lock)) {
            throw new HisException("正在提交预约信息，请勿重复提交");
        }

        log.info("开始插入预约信息，预约信息为：{}，用户 id：{}", request, customerId);

        // 1.2 校验请求参数
        validateAppointmentInfo(request, customerId);

        // 2.1 执行 lua 脚本，校验当天预约人数是否已满，通过 redis 限流
        String lua =
                " local key = KEYS[1] " +
                " if redis.call('EXISTS', key) == 0 then " +                                    // 若 key 不存在则初始化当天
                "   local max = redis.call('GET', key[2]) " +                                    // 获取系统配置的最大预约人数
                "   redis.call('HSET', key, 'realNum', 0) " +                                   // 初始化已预约人数
                "   redis.call('HSET', key, 'maxNum', max) " +                                  // 初始化最大预约人数
                "   redis.call('EXPIRE', key, 90000) " +                                        // 设置过期时间（25小时）
                " end " +
                " local real = tonumber(redis.call('HGET', key, 'realNum') or '0') " +          // 获取当前预约人数
                " local max = tonumber(redis.call('HGET', key, 'maxNum') or '200') " +          // 获取最大人数
                " if real < max then " +                                                        // 判断是否还有名额
                "   redis.call('HINCRBY', key, 'realNum', 1) " +                                // 名额未满 +1
                "   return max " +                                                              // 返回 max 表示成功，用于 restrictionEntity 不存在时设置，不用到 redis 再查一遍
                " else " +
                "   return 0 " +                                                                // 返回 0 表示已满
                " end";

        String redisKey = "his:appointment:" + request.getAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 2.2 执行 lua 脚本
        Long luaResult = redisTemplate.execute(
                new DefaultRedisScript<>(lua, Long.class),
                Arrays.asList(redisKey, "setting:appointment_number")
        );

        // 如果 Lua 返回 0 则名额已满
        if (luaResult == null || luaResult == 0) {
            return "full";
        }

        try {
            // SQL: UPDATE ... SET actual = actual + 1 WHERE date = ? AND actual < limit
            // 3.1 直接先执行更新，若不存在则 updateRows = 0，再插入
            int updateRows = restrictionService.getBaseMapper().update(null,
                    new LambdaUpdateWrapper<AppointmentRestrictionEntity>()
                            .eq(AppointmentRestrictionEntity::getAppointmentDate, request.getAppointmentDate())
                            .lt(AppointmentRestrictionEntity::getActualAppointment, luaResult.intValue())           // 防止 Redis 失效导致超卖
                            // 原子更新实际预约人数（SET actual_appointment = actual_appointment + 1 会获取行锁，然后读取数据，完成写入后释放锁）
                            .setSql("actual_appointment = actual_appointment + 1"));

            // 3.2 更新失败，说明记录不存在或预约人数已满，查询判断是哪种情况
            if (updateRows == 0) {
                AppointmentRestrictionEntity existEntity = restrictionService.getOne(
                        new LambdaQueryWrapper<AppointmentRestrictionEntity>()
                                .eq(AppointmentRestrictionEntity::getAppointmentDate, request.getAppointmentDate())
                );

                // 3.2.1 记录存在但更新失败 -> 预约人数已满
                if (existEntity != null) {
                    return "full";
                }

                // 3.2.2 记录不存在，执行插入
                AppointmentRestrictionEntity restrictionEntity = new AppointmentRestrictionEntity();
                Integer maxNum = luaResult.intValue();

                restrictionEntity.setAppointmentDate(request.getAppointmentDate());
                restrictionEntity.setActualAppointment(1);                                  // 新纪录，实际预约人数只有当前插入的记录
                restrictionEntity.setEverydayLimit(maxNum);
                restrictionEntity.setActualLimit(maxNum);
                restrictionEntity.setCreateTime(LocalDateTime.now());

                try {
                    // 因为 existEntity == null，有多个线程都进入时多个线程都认为 existEntity == null，若有某个线程 save 成功，其他线程再执行时，
                    // 由于 appointment_date 唯一，抛出 DuplicateKeyException，不捕获会导致最下面的 catch 捕获到异常，导致 redis 回滚，然而数据库中实际预约人数因为异常并没有增加
                    restrictionService.save(restrictionEntity);
                    log.info("数据库中没有预约当天的限制记录，新建数据插入到数据库中，restrict：{}", restrictionEntity);
                } catch (DuplicateKeyException e) {
                    // 捕获唯一键冲突，说明其他线程抢先插入了，此时记录已存在，重新尝试更新
                    updateRows = restrictionService.getBaseMapper().update(null,
                            new LambdaUpdateWrapper<AppointmentRestrictionEntity>()
                                    .eq(AppointmentRestrictionEntity::getAppointmentDate, request.getAppointmentDate())
                                    .lt(AppointmentRestrictionEntity::getActualAppointment, luaResult.intValue())
                                    .setSql("actual_appointment = actual_appointment + 1")
                    );
                    if (updateRows == 0) {
                        return "full";
                    }
                }
            }

            // 4.1 创建新的预约记录
            AppointmentEntity appointmentEntity = new AppointmentEntity();
            BeanUtil.copyProperties(request, appointmentEntity);

            appointmentEntity.setUuid(IdUtil.simpleUUID().toUpperCase());
            appointmentEntity.setSex(IdcardUtil.getGenderByIdCard(request.getPid()) == 1 ? "男" : "女");
            appointmentEntity.setOrderId(request.getOrderId());
            appointmentEntity.setName(request.getName());

            String birthByIdCard = IdcardUtil.getBirthByIdCard(request.getPid());
            appointmentEntity.setBirthday(LocalDate.parse(birthByIdCard, DateTimeFormatter.BASIC_ISO_DATE));            // 将生日转换为 LocalDate

            appointmentEntity.setCreateTime(LocalDateTime.now());
            appointmentEntity.setStatus(CheckInStatusEnum.NOT_CHECK_IN.getCode());                                      // 设置为未签到状态

            // 4.2 插入预约记录
            int rows = appointmentDao.insert(appointmentEntity);
            if (rows != 1) {
                log.error("预约记录插入失败，预约日期：{}，预约客户 id：{}，姓名：{}，预约数据：{}",
                        request.getAppointmentDate(), customerId, request.getName(), appointmentEntity);
                throw new RuntimeException("预约记录插入失败");
            }

            // 5. 修改订单状态为已预约
            rows = orderDao.update(null, new LambdaUpdateWrapper<OrderEntity>()
                    .eq(OrderEntity::getId, request.getOrderId())
                    .eq(OrderEntity::getStatus, OrderStatusEnum.PAID.getCode())
                    .set(OrderEntity::getStatus, OrderStatusEnum.APPOINTED.getCode())
            );

            if (rows == 0) {
                log.error("订单记录更新失败，订单 id：{}，用户 id：{}，预约体检人姓名：{}",
                        request.getOrderId(), customerId, request.getName());
                throw new RuntimeException("订单状态更新失败");
            }

            return "success";
        } catch (Exception e) {
            // 6. 如果数据库操作失败，恢复 redis 实际预约人数（-1）
            redisTemplate.opsForHash().increment(redisKey, "realNum", -1);
            // 抛出异常让事务回滚
            log.error("预约失败，预约日期：{}，预约客户 id：{}，姓名：{}",
                    request.getAppointmentDate(), customerId, request.getName(), e);
            throw new RuntimeException(e);
        }
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
        Long orderCount = orderDao.selectCount(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getId, request.getOrderId()).eq(OrderEntity::getCustomerId, customerId)
        );
        if (orderCount != 1) {
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

        // 4. 校验是否在今天已经有过预约
        Long appointmentCount = appointmentDao.selectCount(new LambdaQueryWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getPid, request.getPid())
                .eq(AppointmentEntity::getAppointmentDate, request.getAppointmentDate())
        );
        if (appointmentCount != 0) {
            throw new HisException("预约失败，今天已经预约过");
        }

        // 5. 校验预约时间是否在 30 天内预约
        boolean isIn = !appointmentDate.isBefore(startDate) && !appointmentDate.isAfter(endDate);
        if (!isIn) {
            throw new HisException("预约失败，可预约日期范围为明天起至" + endDate + "（共30天）");
        }
    }
}
