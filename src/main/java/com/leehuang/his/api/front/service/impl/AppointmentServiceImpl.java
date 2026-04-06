package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.IdcardUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehuang.his.api.common.enums.AppointmentStatusEnum;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    
    private final StringRedisTemplate stringRedisTemplate;

    private final AppointmentDao appointmentDao;

    private final AppointmentRestrictionService restrictionService;

    private final OrderDao orderDao;

    private final MinioProperties minioProperties;

    /**
     * 插入新预约
     * @param request 预约请求参数
     * @param customerId 客户 ID
     * @return "success" | "full"       返回插入预约记录数据状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String insertAppointment(AppointmentRequest request, Integer customerId) {

        // 1. Redis 防重复点击（防抖），构建防重复 key（同一个订单 5 秒内只能点一次）
        String repeatKey = "appointment:repeat:" + request.getOrderId();

        // SETNX：不存在才设置成功（5 秒过期）
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(repeatKey, "1", 5, TimeUnit.SECONDS);

        // 如果获取锁失败，说明用户重复点击
        if (Boolean.FALSE.equals(lock)) {
            throw new HisException("正在提交本订单的预约信息，请勿重复提交");
        }

        log.info("开始插入预约信息，订单 ID: {}, 用户 ID: {}", request.getOrderId(), customerId);

        // 2. 校验业务参数，基础校验（订单是否存在、身份证格式等）
        validateAppointmentInfo(request, customerId);

        // 3. Redis + Lua 限流（控制每天预约人数）
        // 构建当天预约 key
        String dateStr = request.getAppointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String redisKey = "his:appointment:" + dateStr;                 // 当天的预约数据 key
        String configKey = "setting:appointment_number";                // 系统设置中的默认每日最大预约数 key

        // 执行 Lua 脚本（预占名额）
        Long luaResult = executeLimitLua(redisKey, configKey);

        // 如果 Lua 返回 0，说明 Redis 层面名额已满
        if (luaResult == null || luaResult == 0) {
            return "full";
        }

        // redis 成功操作后，在数据库事务执行前，提前绑定好事务回滚时的善后处理
        registerRedisRollbackCallback(redisKey);

        try {
            // 4. 数据库限流表更新（防 Redis 失效，双重保险）
            // 返回 true 表示更新/插入成功，返回 false 表示数据库层面已满
            boolean restrictionSuccess = handleRestrictionUpdate(request.getAppointmentDate(), luaResult.intValue());

            if (!restrictionSuccess) {
                // 数据库中预约人数已满，回滚 lua 脚本中的 Redis 计数 +1 操作
                // 手动回滚是因为此时并未抛出异常，事务会正常提交，不会触发上面的 rollback 回调
                stringRedisTemplate.opsForHash().increment(redisKey, "realNum", -1);
                return "full";
            }

            // 5. 插入预约记录
            AppointmentEntity appointmentEntity = buildAppointmentEntity(request);
            int rows = appointmentDao.insert(appointmentEntity);
            if (rows != 1) {
                log.error("预约记录插入失败，预约日期：{}，预约客户 id：{}，姓名：{}，预约数据：{}",
                        request.getAppointmentDate(), customerId, request.getName(), appointmentEntity);
                throw new RuntimeException("预约记录插入失败");
            }

            // 6. 修改订单状态（原子操作，并发安全）
            // 每次预约时，appointed_num + 1 并判断是否等于订单中的商品数，决定订单状态，使用数据库行锁保证并发下数据准确
            // 注：update(null, wrapper) 实体为 null，插件无法获取旧版本号，所以乐观锁失效，只有传入实体类或使用 updateById 时才生效
            int updateOrderRows = orderDao.update(null, new LambdaUpdateWrapper<OrderEntity>()
                    .eq(OrderEntity::getId, request.getOrderId())
                    // 已支付(PAID) -> 部分预约(APPOINTED) -> 全部预约(ALLAPPOINTED)
                    .in(OrderEntity::getStatus, OrderStatusEnum.PAID.getCode(), OrderStatusEnum.APPOINTED.getCode())
                    .apply("appointed_num < number")                    // 防止超额预约：已预约数必须小于商品总数，apply 在 Wrapper 里拼原生 SQL 片段到 WHERE 条件中
                    // 原子更新：计数器 +1，同时通过 CASE WHEN 判断最终状态，setSql 在 UpdateWrapper / LambdaUpdateWrapper 里拼原生 SQL 到 SET 子句
                    .setSql(
                            " appointed_num = appointed_num + 1, " +
                            " status = CASE " +
                            "   WHEN appointed_num = number THEN " + OrderStatusEnum.ALLAPPOINTED.getCode() +
                            "   ELSE " + OrderStatusEnum.APPOINTED.getCode() +
                            " END"
            ));

            // 订单更新失败
            if (updateOrderRows == 0) {
                log.error("订单记录更新失败，订单 id：{}，用户 id：{}，预约体检人姓名：{}", request.getOrderId(), customerId, request.getName());
                throw new HisException("订单状态更新失败");
            }

            return "success";

        } catch (Exception e) {
            // 捕获异常，事务回滚，Redis 的回滚在 registerRedisRollbackCallback 中统一处理
            log.error("预约过程发生异常，触发事务回滚，订单 ID: {}", request.getOrderId(), e);
            throw new HisException("预约系统繁忙，请稍后重试", e);
        }
    }


    /**
     * 执行限流 Lua 脚本
     * @param redisKey      每日预约数据在 redis 中的 key
     * @param configKey     redis 存储的系统设置中每日最大预约人数默认值的 key
     * @return
     */
    private Long executeLimitLua(String redisKey, String configKey) {
        String luaScript =
                        " local key = KEYS[1] " +
                        " local configKey = KEYS[2] " +
                        " if redis.call('EXISTS', key) == 0 then " +                                    // 若 redisKey 不存在则初始化当天的预约数据
                        "   local max = redis.call('GET', configKey) " +                                // 获取系统配置中设置的默认最大预约人数
                        "   if not max then max = '200' end " +                                         // 默认值 200
                        "   redis.call('HSET', key, 'realNum', 0) " +                                   // 初始化已预约人数
                        "   redis.call('HSET', key, 'maxNum', max) " +                                  // 初始化最大预约人数
                        "   redis.call('EXPIRE', key, 90000) " +                                        // 设置过期时间 90000s = 25h
                        " end " +
                        " local real = tonumber(redis.call('HGET', key, 'realNum') or '0') " +          // 获取已预约人数
                        " local max = tonumber(redis.call('HGET', key, 'maxNum') or '200') " +          // 获取最大预约人数
                        " if real < max then " +                                                        // 判断是否还有名额可以预约
                        "   redis.call('HINCRBY', key, 'realNum', 1) " +                                // 还有名额，已预约人数 +1
                        "   return max " +                                                              // 返回 max 表示成功，用于 restrictionEntity 不存在时设置，不用到 redis 再查一遍
                        " else " +
                        "   return 0 " +                                                                // 预约人数已满，返回 0
                        " end";

        // 执行 lua 脚本
        return stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                Arrays.asList(redisKey, configKey)
        );
    }

    /**
     * 注册事务回滚回调：确保 DB 回滚时 Redis 也回滚
     * 事务同步器的激活条件：确保当前线程确实存在一个活跃的事务（即 @Transactional 生效），否则注册不会生效
     * @param redisKey 每日预约数据的 Redis Key
     */
    private void registerRedisRollbackCallback(String redisKey) {
        // 通过 .registerSynchronization() 往 TransactionSynchronizationManager（事务同步管理器） 的事务回调列表中注册事务同步回调
        // 事务同步管理器是用于管理当前线程绑定的资源和管理当前线程的事务同步回调
        // 事务提交或回滚时，Spring 会从事务同步管理器的事务同步列表中里拿出所有 TransactionSynchronization，按注册顺序调用它们的 beforeCommit / afterCommit / afterCompletion 等方法
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // TransactionSynchronization 是一个回调接口，用来在事务的各个阶段插入自定义逻辑，有以下几个关键方法：
            // beforeCommit(boolean readOnly)：事务提交前调用。      beforeCompletion()：事务完成前调用（无论提交还是回滚）。
            // afterCommit()：事务成功提交后调用。                    afterCompletion(int status)：事务提交或回滚后调用。此时原事务的数据库连接已经释放，事务上下文已被清理。不建议继续操作数据库
            // 事务最终结束时，Spring 会回调这个 afterCompletion 方法，只要 DB 事务回滚，就会触发 afterCompletion 方法
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    log.warn("事务回滚，自动补偿 Redis 预约人数：{}", redisKey);
                    try {
                        stringRedisTemplate.opsForHash().increment(redisKey, "realNum", -1);
                    } catch (Exception ex) {
                        log.error("Redis 补偿失败！Key: {}", redisKey, ex);
                    }
                }
            }
        });
    }

    /**
     * 处理数据库限流表更新
     * @param date      预约日期
     * @param maxLimit  当天最大预约数
     * @return          true-成功；false-已满
     */
    private boolean handleRestrictionUpdate(LocalDate date, int maxLimit) {
        // 1. 更新实际预约人数，日期为传入日期且当前预约人数小于最大预约人数
        int updateRows = restrictionService.getBaseMapper().update(null,
                new LambdaUpdateWrapper<AppointmentRestrictionEntity>()
                        .eq(AppointmentRestrictionEntity::getAppointmentDate, date)
                        .lt(AppointmentRestrictionEntity::getActualAppointment, maxLimit)       // 防止 Redis 失效导致超卖
                        // 原子更新实际预约人数（SET actual_appointment = actual_appointment + 1 会获取行锁，然后读取数据，完成写入后释放锁）
                        .setSql("actual_appointment = actual_appointment + 1")
        );

        if (updateRows > 0) {
            return true; // 更新成功
        }

        // 2. 更新失败，两种原因（数据不存在或预约人数已满），获取记录是否存在
        @SuppressWarnings("unchecked")
        AppointmentRestrictionEntity existEntity = restrictionService.getOne(
                new LambdaQueryWrapper<AppointmentRestrictionEntity>()
                        .select(AppointmentRestrictionEntity::getId)
                        .eq(AppointmentRestrictionEntity::getAppointmentDate, date)
        );

        // 2.1 记录存在但更新失败 -> 说明预约人数已满
        if (existEntity != null) {
            return false;
        }

        // 2.2 记录不存在，插入新数据
        AppointmentRestrictionEntity newEntity = new AppointmentRestrictionEntity();
        newEntity.setAppointmentDate(date);
        newEntity.setActualAppointment(1);
        newEntity.setEverydayLimit(maxLimit);
        newEntity.setActualLimit(maxLimit);
        newEntity.setCreateTime(LocalDateTime.now());

        try {
             //因为 existEntity == null，有多个线程都进入时多个线程都认为 existEntity == null，若有某个线程 save 成功，其他线程再执行时，
            // 由于 appointment_date 唯一，会抛出 DuplicateKeyException，此时只需要修改实际预约人数 actual_appointment 计数即可
            restrictionService.save(newEntity);
            log.debug("数据库中没有预约当天的限制记录，新建数据插入到数据库中，restrict：{}", newEntity);
            return true;
        } catch (DuplicateKeyException e) {
            // 并发插入冲突，修改 actual_appointment 计数
            int retryRows = restrictionService.getBaseMapper().update(null,
                    new LambdaUpdateWrapper<AppointmentRestrictionEntity>()
                            .eq(AppointmentRestrictionEntity::getAppointmentDate, date)
                            .lt(AppointmentRestrictionEntity::getActualAppointment, maxLimit)
                            .setSql("actual_appointment = actual_appointment + 1")
            );
            // 更新失败为 false，说明预约人数已满
            return retryRows > 0;
        }
    }

    /**
     * 构建预约记录
     * @param request
     * @return          预约记录数据
     */
    private AppointmentEntity buildAppointmentEntity(AppointmentRequest request) {
        AppointmentEntity entity = new AppointmentEntity();
        BeanUtil.copyProperties(request, entity);

        entity.setUuid(IdUtil.simpleUUID().toUpperCase());
        entity.setOrderId(request.getOrderId());
        entity.setName(request.getName());

        try {
            entity.setSex(IdcardUtil.getGenderByIdCard(request.getPid()) == 1 ? "男" : "女");
            String birth = IdcardUtil.getBirthByIdCard(request.getPid());
            entity.setBirthday(LocalDate.parse(birth, DateTimeFormatter.BASIC_ISO_DATE));
        } catch (Exception e) {
            log.error("身份证解析异常，pid: {}", request.getPid(), e);
            throw new HisException("身份证信息格式错误");
        }

        entity.setCreateTime(LocalDateTime.now());
        entity.setStatus(AppointmentStatusEnum.NOT_CHECK_IN.getCode());
        return entity;
    }


    /**
     * 校验请求数据
     * @param request
     * @param customerId
     */
    private void validateAppointmentInfo(AppointmentRequest request, Integer customerId ) {

        OrderEntity orderEntity = orderDao.selectOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getId, request.getOrderId()));

        // 1. 判断订单是否存在
        if (orderEntity == null) {
            throw new HisException("预约失败，该订单不存在");
        }

        // 2. 校验预约客户是否拥有该订单
        if (!Objects.equals(orderEntity.getCustomerId(), customerId)) {
            throw new HisException("预约失败，该订单不属于预约用户");
        }

        // 3. 校验预约商品是否超出订单商品数（快速校验，后面 DB 插入时兜底）
        if (orderEntity.getAppointedNum() >= orderEntity.getNumber()) {
            throw new HisException("预约失败，预约次数超出商品订单数量");
        }

        // 4. 校验身份证号
        if (!IdcardUtil.isValidCard18(request.getPid())) {
            throw new HisException("预约失败，身份证号无效");
        }

        // 5. 校验是否在今天已经有过预约
        Long appointmentCount = appointmentDao.selectCount(new LambdaQueryWrapper<AppointmentEntity>()
                .eq(AppointmentEntity::getPid, request.getPid())
                .eq(AppointmentEntity::getAppointmentDate, request.getAppointmentDate())
        );
        if (appointmentCount != 0) {
            throw new HisException("预约失败，今天已经预约过");
        }

        // 6. 校验预约时间是否在 30 天内预约
        // 获取 30 天内的日期
        LocalDate appointmentDate = request.getAppointmentDate();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(30);

        boolean isIn = !appointmentDate.isBefore(startDate) && !appointmentDate.isAfter(endDate);
        if (!isIn) {
            throw new HisException("预约失败，可预约日期范围为明天起至" + endDate + "（共30天）");
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

}
