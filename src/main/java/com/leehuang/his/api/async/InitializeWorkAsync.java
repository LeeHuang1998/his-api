package com.leehuang.his.api.async;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehuang.his.api.db.dao.AppointmentRestrictionDao;
import com.leehuang.his.api.db.dao.SystemDao;
import com.leehuang.his.api.db.entity.AppointmentRestrictionEntity;
import com.leehuang.his.api.db.entity.SystemEntity;
import com.leehuang.his.api.handler.FlowRegulationJobHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InitializeWorkAsync {
    private final StringRedisTemplate redisTemplate;

    private final SystemDao systemDao;

    private final AppointmentRestrictionDao appointmentRestrictionDao;

    private final FlowRegulationJobHandler flowRegulationJobHandler;

    @Async("AsyncTaskExecutor")
    @Transactional
    public void initCacheAppointmentData() {

        try {
            // 加载全局设置
            this.loadSystemSetting();

            // 生成未来 30 天的体检日程缓存
            this.createAppointmentCache();

            // 缓存调流规则
            flowRegulationJobHandler.initFlowSystem();

            log.info("【系统初始化】缓存初始化任务执行完成");

        } catch (Exception e) {
            // 异步任务异常集中兜底，防止静默失败
            log.error("【系统初始化】缓存初始化失败", e);
            throw e;
        }

    }

    /**
     * 将系统设置数据缓存
     */
    private void loadSystemSetting() {
        // 1. 获取所有的设置数据
        List<SystemEntity> list = systemDao.selectList(new LambdaQueryWrapper<>());

        Map<String, String> map = new HashMap<>(list.size());
        for (SystemEntity entity : list) {
            map.put("setting:" + entity.getItem(), entity.getValue());
        }

        // 2. 将设置数据存入缓存，使用 multiSet 批量插入，减少网络 IO
        if (MapUtil.isNotEmpty(map)) {
            redisTemplate.opsForValue().multiSet(map);
        }
        log.debug("系统设置缓存成功，共加载 {} 条设置", list.size());
    }

    /**
     *  生成未来 30 天的预约数据缓存
     */
    private void createAppointmentCache() {
        // 1. 未来 30 天，从明天开始，到明天的 30 天后（即今天的 31 天后）
        DateTime startDate = DateUtil.tomorrow();
        DateTime endDate = startDate.offsetNew(DateField.DAY_OF_MONTH, 30);
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);

        log.info("开始生成未来 30 天体检预约缓存，日期范围: {} 至 {}", startDate.toDateStr(), endDate.toDateStr());

        // 2. 从数据库中获取未来 30 天的预约限制数据
        LocalDateTime queryStart = startDate.toLocalDateTime().toLocalDate().atStartOfDay();
        LocalDateTime queryEnd = endDate.toLocalDateTime().toLocalDate().plusDays(1).atStartOfDay();
        List<AppointmentRestrictionEntity> restrictionEntities =
                appointmentRestrictionDao.selectList(
                        new LambdaQueryWrapper<AppointmentRestrictionEntity>()
                                .ge(AppointmentRestrictionEntity::getAppointmentDate, queryStart)
                                .lt(AppointmentRestrictionEntity::getAppointmentDate, queryEnd)
                );

        // 3. 将查询结果转换为以 LocalDate 为 Key 的Map，方便快速查找
        Map<LocalDate, AppointmentRestrictionEntity> restrictionMap = restrictionEntities.stream()
                .collect(Collectors.toMap(
                        AppointmentRestrictionEntity::getAppointmentDate,
                        entity -> entity
                ));

        // 4. 将未来 30 天的缓存预约数据缓存到 redis 中
        range.forEach(currentDate -> {

            // 4.1 获取当前日期，将遍历的数据转换为 LocalDate 与 map 中的 key 进行比较
            LocalDate currentLocalDate = currentDate.toLocalDateTime().toLocalDate();

            // 4.2 从缓存中取出设置的每天限制值（若没有设置预约人数，则使用默认值）
            String value = redisTemplate.opsForValue().get("setting:appointment_number");
            int maxNum = value != null ? Integer.parseInt(value) : 200;                     // 提供默认值
            // 实际预约人数
            int realNum = 0;

            // 4.3 比较当前日期是否在 map 中存在，若存在则使用实际设置和实际的预约人数，若不存在则使用默认值
            AppointmentRestrictionEntity entity = restrictionMap.get(currentLocalDate);
            if (entity != null) {
                maxNum = entity.getActualLimit();
                realNum = entity.getActualAppointment();
                log.info("日期[{}]使用数据库中的数据: maxNum={}, realNum={}", currentDate.toDateStr(), maxNum, realNum);
            }

            // 5. 设置缓存 (使用更统一的Key和精确的过期时间)
            HashMap<String, Object> cache = new HashMap<>();
            cache.put("maxNum", String.valueOf(maxNum));
            cache.put("realNum", String.valueOf(realNum));
            String key = "his:appointment:" + currentDate.toDateStr();
            redisTemplate.opsForHash().putAll(key, cache);

            // 设置缓存过期时间为当前日期的下一天凌晨
            LocalDateTime expireTime = currentLocalDate.plusDays(1).atStartOfDay();
            Date expireDate = Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant());
            redisTemplate.expireAt(key, expireDate);
        });

        log.info("未来 30 天体检人数缓存成功");
    }


}
