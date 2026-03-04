package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leehuang.his.api.common.constants.redis.FlowRegulationConstants;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.dao.AppointmentDao;
import com.leehuang.his.api.db.dao.CheckupResultDao;
import com.leehuang.his.api.db.dao.FlowRegulationDao;
import com.leehuang.his.api.db.entity.AppointmentEntity;
import com.leehuang.his.api.db.entity.FlowRegulationEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.CheckupProgressDTO;
import com.leehuang.his.api.mq.message.FlowTimeoutMessage;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.PlaceVO;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.RealTimeQueueDataVO;
import com.leehuang.his.api.mis.service.MisFlowRegulationService;
import com.leehuang.his.api.mis.service.SystemService;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import com.leehuang.his.api.mis.dto.flowRegulation.request.FlowRegulationPageRequest;
import com.leehuang.his.api.mis.dto.flowRegulation.request.FlowRegulationRequest;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.FlowRegulationPageVO;
import com.leehuang.his.api.mq.producer.QueueDelayProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("MisFlowRegulationService")
@RequiredArgsConstructor
@Slf4j
public class MisFlowRegulationServiceImpl extends ServiceImpl<FlowRegulationDao, FlowRegulationEntity> implements MisFlowRegulationService {

    private final FlowRegulationDao flowRegulationDao;

    private final SystemService systemService;

    // 涉及 ZSET 和 lua 脚本的 redis 操作，或缓存的数据为 String 类型，用 stringRedisTemplate
    private final StringRedisTemplate stringRedisTemplate;

    // 涉及对象直接缓存的使用 redisTemplate
    private final RedisTemplate<String, Object> redisTemplate;

    private final CheckupResultDao checkupResultDao;

    private final QueueDelayProducer queueDelayProducer;

    private final AppointmentDao appointmentDao;

    /**
     * 获取人员调流页面科室列表
     * @return
     */
    @Override
    public List<PlaceVO> searchPlaceList() {
        return flowRegulationDao.selectList(new LambdaQueryWrapper<FlowRegulationEntity>().eq(FlowRegulationEntity::getIsDeleted, 0))
                .stream().map(entity -> {
                    PlaceVO placeVO = new PlaceVO();
                    placeVO.setId(entity.getId());
                    placeVO.setPlace(entity.getPlace());
                    return placeVO;
                }).collect(Collectors.toList());
    }

    /**
     * 获取调流模式，判断是否为自动调流模式
     * @param item  自动调流模式字符串
     * @return
     */
    @Override
    public boolean searchFlowRegulationMode(String item) {
        // true 为自动调流模式，false 为手动调流模式
        return "true".equalsIgnoreCase(systemService.getSystemSettingItemValue(item).trim());
    }

    /**
     * 获取调流页面分页数据
     * @param request
     * @return
     */
    @Override
    public PageUtils<FlowRegulationPageVO> searchFlowRegulationPage(FlowRegulationPageRequest request) {

        // 1. 构建分页条件
        Page<FlowRegulationEntity> page = new Page<>(request.getPage(), request.getLength());

        // 2. 查询条件
        LambdaQueryWrapper<FlowRegulationEntity> queryWrapper = new LambdaQueryWrapper<>();

        // 3. 查询参数
        if (request.getId() != null) {
            queryWrapper.eq(FlowRegulationEntity::getId, request.getId());
        }
        if (StringUtils.hasText(request.getBlueUuid())) {
            queryWrapper.eq(FlowRegulationEntity::getBlueUuid, request.getBlueUuid());
        }

        // 逻辑删除状态不为 1 的数据
        queryWrapper.ne(FlowRegulationEntity::getIsDeleted, 1);

        // 4. 执行分页查询和总数查询
        Page<FlowRegulationEntity> entityPage = flowRegulationDao.selectPage(page, queryWrapper);
        List<FlowRegulationEntity> entityList = entityPage.getRecords();
        long totalCount = entityPage.getTotal();

        // 5. 转换 vo
        List<FlowRegulationPageVO> pageVOS = entityList.stream()
                .map(entity -> BeanUtil.copyProperties(entity, FlowRegulationPageVO.class)).collect(Collectors.toList());

            return new PageUtils<>(totalCount, request.getLength(), request.getPage(), pageVOS);
    }

    /**
     * 添加调流规则
     * @param request   新增调流规则的具体参数
     * @return
     */
    @Override
    @Transactional
    public int insertFlowRegulation(FlowRegulationRequest request) {
        FlowRegulationEntity entity = BeanUtil.copyProperties(request, FlowRegulationEntity.class);
        int rows = flowRegulationDao.insert(entity);
        // 向缓存中添加数据
        if (rows > 0 && entity.getPlace() != null) {
            redisTemplate.opsForHash().put(
                    FlowRegulationConstants.FLOW_REGULATION,
                    entity.getId().toString(),
                    entity
            );

            // 同时初始化 ZSET 中的排队人数（如果该科室不存在）
            stringRedisTemplate.opsForZSet().addIfAbsent(
                    FlowRegulationConstants.FLOW_PLACE_RANK,
                    entity.getId().toString(),
                    0.0
            );
        }
        return rows;
    }

    /**
     *  根据 id 查询调流规则
     * @param id
     * @return
     */
    @Override
    public FlowRegulationPageVO searchFlowRegulationById(Integer id) {
        FlowRegulationEntity entity = flowRegulationDao.selectById(id);
        if (entity != null) {
            return BeanUtil.copyProperties(entity, FlowRegulationPageVO.class);
        } else {
            throw new HisException("没有找到对应的调流规则");
        }
    }

    /**
     * 更新调流规则
     * @param request   更新调流规则的具体参数
     * @return
     */
    @Override
    @Transactional
    public int updateFlowRegulation(FlowRegulationRequest request) {
        FlowRegulationEntity entity = BeanUtil.copyProperties(request, FlowRegulationEntity.class);
        int rows = flowRegulationDao.updateById(entity);

        if (rows > 0 && entity.getPlace() != null) {
            // 同步更新 Redis Hash，ZSET 不用更新，因为 member 为 id 且只记录排队人数
            redisTemplate.opsForHash().put(
                    FlowRegulationConstants.FLOW_REGULATION,
                    entity.getId(),
                    entity
            );
        }
        return rows;
    }


    /**
     * 某科室检查完成后，更新调流并返回下一推荐科室
     * @param uuid                  体检单唯一编号
     * @param customerName          体检人姓名
     * @param finishedPlace         当前提交检查结果的科室名
     * @param placeId               当前提交检查结果的科室 id
     * @return
     */
    @Override
    public NextPlaceVO finishPlaceAndRecommendNext(String uuid, String customerName, String finishedPlace, Integer placeId) {

        // 1. 构造 Redis Key
        // 这个 key 中保存着所有该体检订单已完成的科室
        String finishedKey = FlowRegulationConstants.CHECKUP_FINISHED_PREFIX + uuid;

        // 2. 判断该体检单是否已经完成过该科室（返回值为 1 时，表示添加成功，即之前没有提交过该科室的体检结果，防止重复提交）
        boolean firstFinish = Long.valueOf(1).equals(stringRedisTemplate.opsForSet().add(finishedKey, finishedPlace));

        log.info("科室完成状态检查：firstFinish={}, uuid={}, placeId={}, place={}", firstFinish, uuid, placeId, finishedPlace);

        // 3. 获取下一个推荐的科室并返回，当前体检科室第一次完成体检时，才到下一个科室排队，若不是第一次提交结果，那么说明已经在下一个科室排过队了
        NextPlaceVO nextPlaceVO = this.recommendNextPlace(uuid, firstFinish);

        // 4. 只有第一次完成，才更新调流，减少排队人数，否则说明之前已经完成了体检，本次只是体检修改结果，不需要再进行调流
        // ZSET 中的每个元素都是一个二元组，例如该 ZSET 的 key 为 flow:place:rank，那么该 ZSET 中存储的每个元素结构为：(member. score)
        // 其中 member 不能重复，score 可以重复，且自动按 score 排序，若 score 相同，再按 member 字典排序（对整个字符串的逐个字符的 unicode 进行排序，若相同则比较该字符串的下一个字符）
        if (Boolean.TRUE.equals(firstFinish)) {
            // 4.1 查找当前排队状态，提前标记排队已完成，等待 consumer 中校验状态
            String queueKey = FlowRegulationConstants.QUEUE_MAP_PREFIX + uuid + ":" + placeId;

            // 根据 queueKey 查询当前体检单的排队状态，如果状态为 PENDING，则说明该体检单正在排队中，可以进行调流操作
            // 4.2 原子执行 redis 操作：当前科室排队人数 -1，且删除排队人员，标记排队状态为 DONE（mq consumer 中做幂等操作，若已提交体检结果，等待消息过期后 ack 即可）
            String lua = "if redis.call('GET', KEYS[1]) == 'PENDING' then " +          // 若体检单的状态为 PENDING，则执行如下操作
                    " redis.call('ZINCRBY', KEYS[2], -1, ARGV[1]); " +                 // 给 member 为 ARGV[1] 的排队人数 -1
                    " redis.call('ZREM', KEYS[3], ARGV[2]); " +                        // 从 ZSet 排队人员姓名队列中删除该排队人员
                    " redis.call('SET', KEYS[1], 'DONE', 'EX', 600); " +               // 标记为 DONE，防止 MQ 再次回收
                    " return 1; " +
                    " else return 0; end";

            // decremented = 1：脚本执行成功；decremented = 0：queueKey 状态不是 "PENDING"（可能是 "DONE"、null 或其他值）；decremented = null：Lua 脚本执行失败或返回 null
            Long decremented = stringRedisTemplate.execute(
                    new DefaultRedisScript<>(lua, Long.class),
                    Arrays.asList(                                                      // lua 脚本中的 KEYS
                            queueKey,
                            FlowRegulationConstants.FLOW_PLACE_RANK,
                            FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + placeId
                    ),
                    placeId.toString(),                                           // lua 脚本中的 ARGV
                    uuid + ":" + customerName
            );

            if (decremented != null && decremented == 1) {
                log.info("体检单 {} 完成科室 {}，排队人数 -1", uuid, finishedPlace);
            } else {
                log.error("体检单：【{}】， 体检科室：【{}】，提交体检结果操作失败，排队人数 -1 失败，lua 执行结果 decremented = {}", uuid, finishedPlace, decremented);
                throw new HisException("体检单【" + uuid + "】，体检科室 " + finishedPlace + " 提交体检结果操作失败");
            }
        }

        return nextPlaceVO;
    }

    /**
     * 获取下一个推荐的科室：获取当前的体检进度，判断是否已经完成所有体检，若完成则直接返回 null → 没有全部完成，获取未完成的体检科室以及排队人数对象数据，并进行排序
     *  → 排序完成后，获取数据库中存储的科室调流数据，构建 key 为科室名，value 为 科室调流数据的 map，并计算总权重 → 计算最推荐的科室并返回，给最推荐科室的排队人数 + 1
     *  计算方式：判断当前的调流模式，根据模式计算推荐科室，
     *      自动调流模式：根据公式 （当前权重 / 总权重） * （科室最大容量 - 科室当前排队人数） 计算推荐值，取最大值，返回该科室
     *      手动调流模式：在未完成体检科室和排队人数未满的科室中，选取优先级最高的科室
     * @param uuid                  体检单唯一编码
     * @param toPlaceQueue          到下一个科室是否需要排队，只有从未体检过、继续体检功能 或 在第一次完成某项体检后才需要排队
     * @return
     */
    @Override
    public NextPlaceVO recommendNextPlace(String uuid, Boolean toPlaceQueue) {
        // 1. 查询当前体检进度
        CheckupProgressDTO progressDTO = checkupResultDao.getCheckupProgress(uuid);

        // 判断是否所有体检都已完成，若所有体检都完成，不再推荐下一个科室，返回 null
        if (progressDTO.isAllFinished()) {
            return null;
        }

        // 2. 获取所有已完成体检项目的科室
        Map<String, String> finishedPlaceMap = progressDTO.getFinishedPlaces();

        // 3. 从 Redis ZSET 中按 score 排队人数升序获取所有科室
        // rangeWithScores(ZSET key, start, end)：返回的是从已经排好序的 ZSET 中获取指定范围内的元素，且每个元素都包含 score
        // Set<ZSetOperations.TypedTuple<String>> 是 Spring Data Redis 对 ZSET 的 Java 映射
        // TypedTuple<String> 是一个对象，内部包含 {String value; Double score;} 两个属性
        Set<ZSetOperations.TypedTuple<String>> candidates =
                stringRedisTemplate.opsForZSet().rangeWithScores(FlowRegulationConstants.FLOW_PLACE_RANK, 0, -1);

        // 3.1 若为空则直接返回
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // 3.2 本次体检所有应前往体检的科室
        Set<String> allPlacesSet = progressDTO.getAllPlaces().stream().map(String::trim).collect(Collectors.toSet());

        if (allPlacesSet.isEmpty()) {
            throw new HisException("本次体检没有查询到任何体检项，请联系管理员");
        }

        // 3.3 查询所有已启用的科室
        Set<String> allPlaces = flowRegulationDao.selectList(new LambdaQueryWrapper<FlowRegulationEntity>()
                        .in(FlowRegulationEntity::getPlace, allPlacesSet)
                        .eq(FlowRegulationEntity::getIsDeleted, 0 ))
                .stream().map(entity -> entity.getId().toString()).collect(Collectors.toSet());

        // 3.4 从所有已启用科室中筛选出本次体检应该前往的所有科室的排队数据（原来的 candidates 包含所有科室，在这里 filter 后 candidates 只有本次体检套餐且未完成的科室）
        // candidates 中肯定没有已停用的科室，因为在停用时就已经从缓存中删除了
        candidates = candidates.stream()
                .filter(tuple -> allPlaces.contains(tuple.getValue()) && !finishedPlaceMap.containsKey(tuple.getValue()))
                .collect(Collectors.toSet());

        // 4. 从 redis 中获取所有科室的调流数据
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(FlowRegulationConstants.FLOW_REGULATION);

        // 4.1 所有科室的调流配置，构建 Map，key：科室名，value：Function.identity()，表示直接将整个 FlowRegulationEntity 对象作为 Map 的 Value
        Map<String, FlowRegulationEntity> regulationMap;

        // 4.2 判断 map 是否为空，若 redis 中没有值，从数据库中获取，并保存到数据库中（即获取所有科室的调流数据时 Redis 缓存未命中 → DB 回源）
        if (entries.isEmpty()) {
            log.warn("缓存 {} 不存在或为空，尝试从数据库加载，时间：{}", FlowRegulationConstants.FLOW_REGULATION, LocalDateTime.now());
            List<FlowRegulationEntity> regulationList = flowRegulationDao.selectList(new LambdaQueryWrapper<FlowRegulationEntity>()
                    .eq(FlowRegulationEntity::getIsDeleted, 0));
            regulationMap = regulationList.stream()
                    .collect(Collectors.toMap(FlowRegulationEntity::getPlace, Function.identity()));
            // 写入到 redis 缓存中
            redisTemplate.opsForHash().putAll(FlowRegulationConstants.FLOW_REGULATION, regulationMap);
            log.info("从数据库加载所有科室调流数据 {} 成功，并成功缓存到 redis 中，时间：{}", FlowRegulationConstants.FLOW_REGULATION, LocalDateTime.now());
        } else {
            // Redis 缓存命中，根据调流数据获取一个 key 为科室名称，value 为对应科室调流数据 entity 的 Map<String, FlowRegulationEntity>
            regulationMap = entries.entrySet().stream().collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> (FlowRegulationEntity) e.getValue()
                    ));
        }

        // 5. 计算推荐科室
        // 5.1 获取当前的调流模式，若为自动调流模式则为 true，手动调流模式则为 false
        String mode = systemService.getSystemSettingItemValue(FlowRegulationConstants.AUTO_FLOW_REGULATION);

        // 5.2 根据模式计算推荐科室
        NextPlaceVO bestDto = Boolean.parseBoolean(mode)
                ? getRecommendedPlaceByWeight(candidates, regulationMap, finishedPlaceMap)
                : getRecommendedPlaceByPriority(candidates, regulationMap, finishedPlaceMap);

        // 6. 若没有任何可推荐科室
        if (bestDto == null) {
            return null;
        }

        // 7. 若需要排队，则将当前体检单标记为排队中，并记录当前体检单的排队状态，用于医生提交检查结果时标记消息已完成
        if (toPlaceQueue) {
            // 7. 发送延迟消息到 mq 中，在 30 分钟后排队人数 -1（失败直接抛异常）

            // KEYS[n] ：脚本中要用到的 Redis Key，在 redisTemplate.execute 中通过 Arrays.asList(...) 传入，n 代表 List 中第几个元素（从 1 开始）
            // ARGV[n] ：脚本中要用到的参数，在 redisTemplate.execute 中通过 Arrays.asList(...) 后的第三个参数开始传入
            // return 1：固定返回 1 告知 Java 层执行成功
            String lua = "local added = redis.call('ZADD', KEYS[1], 'NX', ARGV[1], ARGV[2]) " +      // ZADD key score member，给当前科室的排队人员名单添加姓名（只有不存在时才添加）
                    "if added == 1 then " +                                                          // 成功添加 member（uuid:排队人员姓名），失败返回 0
                    "redis.call('SET', KEYS[2], 'PENDING', 'EX', ARGV[3]) " +                        // SET key value EX seconds，设置当前体检单排队状态为 PENDING，过期时间 40 分钟
                    "redis.call('ZINCRBY', KEYS[3], 1, ARGV[4]) " +                                  // ZINCRBY key incrementValue member，给当前科室排队人数 +1
                    "redis.call('EXPIRE', KEYS[1], ARGV[5]) " +                                      // expire key seconds，设置排队人员姓名队列过期时间为 23 小时
                    "end " +
                    "return added ";                                                                 // 返回 1，表示添加成功，返回 0 表示添加失败
            try {

                // 7.1. 同时在 redis 中标记该体检处于进行中（消息尚未消费），用于医生提交检查结果时标记消息已完成
                String queueKey = FlowRegulationConstants.QUEUE_MAP_PREFIX + uuid + ":" + bestDto.getId();

                // 原子执行 redis 操作，往科室排队人员队列中添加姓名，添加成功时返回 1，若成功则给当前体检单的排队状态设置为 PENDING，该科室排队人数 +1，并设置排队人员队列过期时间为 23 小时
                Long executeResult = stringRedisTemplate.execute(
                        new DefaultRedisScript<>(lua, Long.class),
                        Arrays.asList(
                                FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + bestDto.getId(),
                                queueKey,
                                FlowRegulationConstants.FLOW_PLACE_RANK
                        ),
                        String.valueOf(System.currentTimeMillis()),                   // 在 ZSET flow:regulation:place_queue:placeId 设置 score 为当前时间戳
                        uuid + ":" + progressDTO.getName(),                                 // 在 ZSET flow:regulation:place_queue:placeId 设置 member 为 uuid:排队人员姓名
                        String.valueOf(TimeUnit.MINUTES.toSeconds(40)),             // 设置排队状态为 PENDING 的 redis数据过期时间为 40 分钟
                        bestDto.getId().toString(),                                         // 在 ZSET flow:regulation:rank 中 member 为传入 place 的 score +1
                        String.valueOf(TimeUnit.HOURS.toSeconds(23))                // 为排队人员姓名队列设置过期时间为 23 小时
                );

                if (executeResult != null && executeResult == 1L) {
                    // 7.2 构建延迟消息
                    FlowTimeoutMessage message = new FlowTimeoutMessage();
                    message.setPlaceId(bestDto.getId());
                    message.setPlace(bestDto.getPlace());
                    message.setUuid(uuid);
                    message.setName(progressDTO.getName());

                    // 7.3 发送 30 分钟后排队人数 -1 的消息
                    queueDelayProducer.sendQueueTimeoutMessage(message, TimeUnit.MINUTES.toMillis(30));

                    log.info("延迟消息发送成功，本次排队人数 +1，下一个科室：place={}, uuid={}", bestDto.getPlace(), uuid);
                } else if (executeResult != null && executeResult == 0L) {
                    log.warn("lua 返回值为 0，已在本科室排队，本次排队人数无需 +1，place={}, uuid={}", bestDto.getPlace(), uuid);
                } else {
                    log.error("lua 执行失败，place={}, uuid={}", bestDto.getPlace(), uuid);
                }
            } catch (Exception e) {
                log.error("延迟消息发送失败，本次排队人数可能无法自动回收，place={}, uuid={}", bestDto.getPlace(), uuid, e);
            }
        }
        return bestDto;
    }

    /**
     * 根据优先级计算推荐科室
     * @param candidates                按照排队人数升序排列的所有科室
     * @param regulationMap             科室调流数据 map，即数据库中存储的 tb_flow_regulation 表的数据
     * @param finishedPlaceMap          当前体检用户已经完成体检的科室
     * @return
     */
    private NextPlaceVO getRecommendedPlaceByPriority(
            Set<ZSetOperations.TypedTuple<String>> candidates,
            Map<String, FlowRegulationEntity> regulationMap,
            Map<String, String> finishedPlaceMap) {

        // 所有的科室数据
        List<FlowRegulationEntity> regulationList = new ArrayList<>(regulationMap.values());

        // 遍历所有科室，找到未完成体检、排队人数未满且优先级最高的科室
        for (ZSetOperations.TypedTuple<String> candidate : candidates) {
            // 1. 获取当前科室名称
            String placeId = candidate.getValue();

            // 2. 筛选掉已经完成的科室
            if (finishedPlaceMap.containsKey(placeId)) {
                // 将已完成的科室从 regulationList 中移除
                regulationList.removeIf(entity -> Objects.equals(entity.getId().toString(), placeId));
                continue;
            }

            // 3. 获取当前循环的科室的数据
            FlowRegulationEntity placeRegulation = regulationMap.get(placeId);

            // 3.1 Redis 中存在，但数据库中不存在该科室数据，直接跳过（防脏数据）
            if (placeRegulation == null) {
                continue;
            }

            // 3.2 筛选掉排队人员已满的科室
            // 当前排队人数（ZSET 中元素的 score）
            if (candidate.getScore() == null) {
                log.warn("调流模式：手动调流，科室 id {}，科室：{} 的排队人数（score）为 null，跳过该科室", placeId, placeRegulation.getPlace());
                continue;               // 或初始化为0
            }

            long realNum = candidate.getScore().longValue();

            // 最大承载人数（r.max_num）
            int maxNum = placeRegulation.getMaxNum();
            // 若已满员，不可再推荐
            if (realNum >= maxNum) {
                regulationList.removeIf(entity -> Objects.equals(entity.getId().toString(), placeId));
                continue;
            }

            // 3.3 给该科室添加排队人数的数据，Math.toIntExact(realNum) 安全地将 Long 转换为 Integer
             placeRegulation.setRealNum(Math.toIntExact(realNum));
        }

        // 4. 判断是否有推荐的科室
        if (regulationList.isEmpty()) {
            return null;
        }

        // 5. 从已经筛选移除掉无效科室的 List 中，获取优先级最高的科室
        FlowRegulationEntity entity = regulationList.stream().max(Comparator.comparing(FlowRegulationEntity::getPriority)).get();

        NextPlaceVO bestDto = new NextPlaceVO();
        bestDto.setId(entity.getId());
        bestDto.setPlace(entity.getPlace());
        // Math.toIntExact(realNum) 安全地将 Long 转换为 Integer
        bestDto.setWaitingCount(entity.getRealNum());

        return bestDto;
    }

    /**
     * 根据权重计算推荐科室
     * @param candidates                按照排队人数升序排列的所有科室
     * @param regulationMap             科室调流数据 map，即数据库中存储的 tb_flow_regulation 表的数据
     * @param finishedPlaceMap          当前体检用户已经完成体检的科室
     * @return
     */
    private NextPlaceVO getRecommendedPlaceByWeight(
            Set<ZSetOperations.TypedTuple<String>> candidates,
            Map<String, FlowRegulationEntity> regulationMap,
            Map<String, String> finishedPlaceMap) {

        // 1. 计算所有科室的权重之和
        int totalWeight = regulationMap.values().stream().mapToInt(FlowRegulationEntity::getWeight).sum();

        // 2.  防御性判断，避免除以 0
        if (totalWeight <= 0) {
            return null;
        }

        // 最推荐的科室
        NextPlaceVO bestDto = null;
        // 该科室的推荐值
        double maxScore = Double.NEGATIVE_INFINITY;

        // 3. 遍历 Redis 中的所有科室，计算推荐值
        for (ZSetOperations.TypedTuple<String> candidate : candidates) {

            // 当前遍历的科室
            String placeId = candidate.getValue();

            // 3.1 跳过已完成体检的科室
            if (finishedPlaceMap.containsKey(placeId)) {
                continue;
            }

            // 3.2 获取该科室的数据
            FlowRegulationEntity placeRegulation = regulationMap.get(placeId);

            // 3.3 Redis 中存在，但数据库中不存在该科室数据，直接跳过（防脏数据）
            if (placeRegulation == null) {
                continue;
            }

            // 3.4 计算科室的推荐值
            // 当前排队人数（ZSET 中元素的 score）
            if (candidate.getScore() == null) {
                log.warn("调流模式：自动调流，科室 id：{}，科室：{} 的排队人数（score）为 null，跳过该科室", placeId, placeRegulation.getPlace());
                continue;               // 或初始化为0
            }
            long realNum = candidate.getScore().longValue();
            // 最大承载人数（r.max_num）
            int maxNum = placeRegulation.getMaxNum();
            // 若已满员，不可再推荐
            if (realNum >= maxNum) {
                continue;
            }
            // 科室权重（r.weight）
            int weight = placeRegulation.getWeight();

            // 计算推荐值
            double recommendScore = ((double) weight / totalWeight) * (maxNum - realNum);

            // 3.5 若当前推荐值更高，则替换
            if (recommendScore > maxScore) {
                maxScore = recommendScore;

                NextPlaceVO dto = new NextPlaceVO();
                dto.setId(placeRegulation.getId());
                dto.setPlace(placeRegulation.getPlace());
                // Math.toIntExact(realNum) 安全地将 Long 转换为 Integer
                dto.setWaitingCount(Math.toIntExact(realNum));

                bestDto = dto;
            }
        }
        return bestDto;
    }

    /**
     * 获取所有科室的实施排队人数
     * @return
     */
    @Override
    public List<RealTimeQueueDataVO> searchRealTimeQueueData() {
        // 1. 从 redis 中获取所有科室的实时排队人数
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet().rangeWithScores(FlowRegulationConstants.FLOW_PLACE_RANK, 0, -1);

        if (tuples == null || tuples.isEmpty()) {
            throw new HisException("没有查询到任何科室，请联系管理员");
        }

        Map<String, FlowRegulationEntity> entiresMap;

        // 2. 从缓存中获取数据
        HashOperations<String, String, FlowRegulationEntity> hashOps = redisTemplate.opsForHash();
        entiresMap = hashOps.entries(FlowRegulationConstants.FLOW_REGULATION);

        // 2.1 若缓存未命中从数据库中获取每个科室的数据（已启用的科室），并缓存到 redis 中
        if (entiresMap.isEmpty()) {
            log.debug("科室数据缓存未命中，从数据库中加载" );
            List<FlowRegulationEntity> entityList = flowRegulationDao
                    .selectList(new LambdaQueryWrapper<FlowRegulationEntity>()
                            .eq(FlowRegulationEntity::getIsDeleted, 0));

            if (entityList.isEmpty()) {
                throw new HisException("没有查询到任何科室，请联系管理员");
            }

            // 2.2 转换成 map，将科室数据缓存到 redis 中
            Map<String, FlowRegulationEntity> loadedMap = entityList.stream()
                    .collect(Collectors.toMap(entity -> entity.getId().toString(), Function.identity()));

            // 将查询出来的数据添加到 entiresMap 中，不要重新给 entiresMap 赋值，而是通过 putAll 改变内容
            // （因为 tuples.stream().map() 是 Lambda 表达式且引用了外部局部变量，而在 Lambda 表达式中要求外部变量在初始化后不能再被重新赋值）
            entiresMap.putAll(loadedMap);

            redisTemplate.opsForHash().putAll(FlowRegulationConstants.FLOW_REGULATION, loadedMap);
        }

        // 3. 各科室实时排队人数和最大排队人数对象构建，并按照 id 升序排列
        return tuples.stream().map(tuple -> {
            Integer id = Integer.parseInt(Objects.requireNonNull(tuple.getValue()));

            RealTimeQueueDataVO dataVO = new RealTimeQueueDataVO();
            dataVO.setId(id);
            dataVO.setPlace(entiresMap.get(id.toString()).getPlace());
            dataVO.setRealNum(Objects.requireNonNull(tuple.getScore()).intValue());
            dataVO.setMaxNum(entiresMap.get(id.toString()).getMaxNum());
            return dataVO;
        }).sorted(Comparator.comparing(RealTimeQueueDataVO::getId)).collect(Collectors.toList());
    }

    /**
     * 根据科室 id，获取该科室实时排队人员名单
     * @param id
     * @return
     */
    @Override
    public List<String> searchQueueByPlace(Integer id) {
        // 从 Redis 中获取
        Set<ZSetOperations.TypedTuple<String>> tuples = 
                stringRedisTemplate.opsForZSet().rangeWithScores(FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + id, 0, -1);
        
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        return tuples.stream().map(tuple -> {
            String value = tuple.getValue();
            assert value != null;
            String[] split = value.split(":");
            return split[1];
        }).collect(Collectors.toList());
    }

    /**
     * 给指定科室添加排队人员
     * @param id       科室 id
     * @param uuid     体检单唯一编号
     * @return
     */
    @Override
    public Boolean addQueuePerson(Integer id, String uuid) {
        // 1. 从数据库中获取当前体检单的体检人信息
        @SuppressWarnings("unchecked")
        AppointmentEntity appointmentEntity = appointmentDao
                .selectOne(new LambdaQueryWrapper<AppointmentEntity>()
                        .select(AppointmentEntity::getName)
                        .eq(AppointmentEntity::getUuid, uuid));

        if (appointmentEntity == null) {
            throw new HisException("未找到体检人信息");
        }

        // 2.1 从缓存中获取科室信息
        FlowRegulationEntity flowRegulationEntity = (FlowRegulationEntity) redisTemplate.opsForHash()
                .get(FlowRegulationConstants.FLOW_REGULATION, id.toString());

        // 2.2 redis 未找到科室数据，从数据库中查询
        if (flowRegulationEntity == null) {
            flowRegulationEntity = flowRegulationDao.selectById(id);

            if (flowRegulationEntity == null) {
                throw new HisException("未找到体检科室信息");
            }

            // 2.3 缓存到 redis 中
            redisTemplate.opsForHash().put(
                    FlowRegulationConstants.FLOW_REGULATION,
                    id.toString(),
                    flowRegulationEntity
            );
            redisTemplate.expire(FlowRegulationConstants.FLOW_REGULATION, 25, TimeUnit.HOURS);
        }

        // 3. 将当前体检单添加到排队队伍中，并给排队人数 +1，排队状态设置为 PENDING
        // 3.1 原子执行 redis 操作，往科室排队人员队列中添加姓名，添加成功时返回 1，
        String lua = "local added = redis.call('ZADD', KEYS[1], 'NX', ARGV[1], ARGV[2]) " +
                "if added == 1 then " +
                "redis.call('SET', KEYS[2], 'PENDING', 'EX', ARGV[3]) " +
                "redis.call('ZINCRBY', KEYS[3], 1, ARGV[4]) " +
                "redis.call('EXPIRE', KEYS[1], ARGV[5]) " +
                "end " +
                "return added ";

        try {
            // 同时在 redis 中标记该体检处于进行中（消息尚未消费），用于医生提交检查结果时标记消息已完成
            String queueKey = FlowRegulationConstants.QUEUE_MAP_PREFIX + uuid + ":" + id;

            // 3.2 执行 lua，若成功则给当前体检单的排队状态设置为 PENDING，该科室排队人数 +1，并设置排队人员队列过期时间为 25 小时
            Long executeResult = stringRedisTemplate.execute(
                    new DefaultRedisScript<>(lua, Long.class),
                    Arrays.asList(
                            FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + id,
                            queueKey,
                            FlowRegulationConstants.FLOW_PLACE_RANK
                    ),
                    String.valueOf(System.currentTimeMillis()),                    // 在 ZSET flow:regulation:place_queue:placeId 设置 score 为当前时间戳
                    uuid + ":" + appointmentEntity.getName(),                            // 在 ZSET flow:regulation:place_queue:placeId 设置 member 为 uuid:排队人员姓名
                    String.valueOf(TimeUnit.MINUTES.toSeconds(40)),              // 设置排队状态为 PENDING 的 redis数据过期时间为 40 分钟
                    id.toString(),                                                       // 在 ZSET flow:regulation:rank 中 member 为传入 place 的 score +1
                    String.valueOf(TimeUnit.HOURS.toSeconds(25))                 // 为排队人员姓名队列设置过期时间为 25 小时
            );

            // 3.2 成功执行 lua 后，发送延迟消息到延迟队列中
            if (executeResult != null && executeResult == 1L) {
                // 构建延迟消息
                FlowTimeoutMessage message = new FlowTimeoutMessage();
                message.setPlaceId(id);
                message.setPlace(flowRegulationEntity.getPlace());
                message.setUuid(uuid);
                message.setName(appointmentEntity.getName());

                // 发送 30 分钟后排队人数 -1 的消息
                queueDelayProducer.sendQueueTimeoutMessage(message, TimeUnit.MINUTES.toMillis(30));
                log.info("手动添加排队人员 addQueuePerson，延迟消息发送成功，本次排队人数 +1，下一个科室：place={}, uuid={}", flowRegulationEntity.getPlace(), uuid);
            } else if (executeResult != null && executeResult == 0L) {
                log.warn("手动添加排队人员 addQueuePerson，lua 返回值为 0，已在本科室排队，本次排队人数无需 +1，place={}, uuid={}", flowRegulationEntity.getPlace(), uuid);
                return false;
            } else {
                log.error("手动添加排队人员 addQueuePerson，lua 执行失败，place={}, uuid={}", flowRegulationEntity.getPlace(), uuid);
                throw new HisException("lua 错误，科室挂号失败，请联系管理员");
            }
        } catch (Exception e) {
            log.error("手动添加排队人员 addQueuePerson，延迟消息发送失败，本次排队人数可能无法自动回收，place={}, uuid={}", flowRegulationEntity.getPlace(), uuid, e);
        }
        return true;
    }

    /**
     * 科室跳过指定排队人员
     * @param id        科室 id
     * @param uuid      体检单唯一编号
     * @return
     */
    @Override
    public Boolean skipQueuePerson(Integer id, String uuid) {

        // 1. 从数据库中获取体检人信息
        @SuppressWarnings("unchecked")
        AppointmentEntity appointmentEntity = appointmentDao
                .selectOne(new LambdaQueryWrapper<AppointmentEntity>()
                        .select(AppointmentEntity::getName)
                        .eq(AppointmentEntity::getUuid, uuid));

        if (appointmentEntity == null) {
            throw new HisException("未找到体检人信息");
        }

        // 2. 从 redis 中移除该体检人数据，给对应科室的排队人数 -1,标记排队状态为 DONE
        String lua =
                " local exists = redis.call('ZSCORE', KEYS[3], ARGV[2]) " +       // 判断该体检单客户是否在该科室的排队队列中，exist 为 false 时表示该 member 不存在
                " if not exists then " +                                          // 不存在则返回 nil，redis.call 会将 Redis 的 nil 回复转换为 Lua 的 false
                "   return -1; " +
                " end; " +
                " if redis.call('GET', KEYS[1]) == 'PENDING' then " +              // 若体检单的状态为 PENDING，则执行如下操作
                " redis.call('ZINCRBY', KEYS[2], -1, ARGV[1]); " +                 // 给 member 为 ARGV[1] 的排队人数 -1
                " redis.call('ZREM', KEYS[3], ARGV[2]); " +                        // 从 ZSet 排队人员姓名队列中删除该排队人员
                " redis.call('SET', KEYS[1], 'DONE', 'EX', 600); " +               // 标记为 DONE，防止 MQ 再次回收
                " return 1; " +
                " else return 0; end";

        // result 的值：1 表示脚本执行成功，0 表示排队状态不为 "PENDING"，-1 表示该人员不在本科室的排队队列中，null 表示 Lua 脚本执行失败或返回 null
        Long result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(lua, Long.class),
                Arrays.asList(                                                      // lua 脚本中的 KEYS
                        FlowRegulationConstants.QUEUE_MAP_PREFIX + uuid + ":" + id,
                        FlowRegulationConstants.FLOW_PLACE_RANK,
                        FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + id
                ),
                id.toString(),                                           // lua 脚本中的 ARGV
                uuid + ":" + appointmentEntity.getName()
        );

        if (result != null && result == 1L) {
            log.info("体检单：【{}】，该人员移除排队队列，科室id: {} 排队人数 -1", uuid, id);
        } else if (result != null && result == -1L) {
            log.debug("体检单：【{}】，该人员不在排队队列中，科室id: {}", uuid, id);
            throw new HisException("体检单【" + uuid + "】，该人员不在本科室的排队队列中");
        } else if (result != null && result == 0L) {
            log.debug("体检单：【{}】，该人员排队状态不为 PENDING，科室id: {}", uuid, id);
            throw new HisException("体检单【" + uuid + "】已处理，无法重复过号");
        } else {
            log.error("科室id: {}，体检单:【{}】，该人员移除排队队列失败，", id, uuid);
            throw new HisException("体检单：【" + uuid + "】，该客户过号失败，请联系管理员");
        }
        return true;
    }

    /**
     * 停用指定科室
     * @param id
     * @return
     */
    @Override
    @Transactional
    public int deactivatePlace(Integer id) {

        // 1. 缓存中是否有人排队，若没有人排队则删除相关的排队缓存，若有则返回 -1 并抛出异常
        String lua = " local count = redis.call('ZCARD', KEYS[1]) " +                   // 查询是否存在 key[1] 这个 ZSET
                     " if count > 0 then " +
                     " return -1 " +                                                    // 存在该 ZSET 时 count > 0，说明还有人在排队，返回 -1
                     " end " +
                     " redis.call('ZREM', KEYS[2], ARGV[1]) " +                         // 删除该 ZSET 中 member 为 ARGV[1] 的元素
                     " redis.call('HDEL', KEYS[3], ARGV[1]) " +                         // 删除该 hash 中 key 为 ARGV[1] 的元素
                     " return 1 ";                                                      // 返回 1 表示删除成功

        Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(lua, Long.class),
                Arrays.asList(
                        FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + id,
                        FlowRegulationConstants.FLOW_PLACE_RANK,
                        FlowRegulationConstants.FLOW_REGULATION
                ),
                id.toString()
        );

        if (result == null || result == -1) {
            log.error("停用科室失败，该科室仍有排队人员，placeId={}", id);
            throw new HisException("停用科室失败，该科室仍有排队人员");
        }

        // 2. 更新数据库数据，将科室状态改为 2（停用）
        int update = flowRegulationDao.update(null, new LambdaUpdateWrapper<FlowRegulationEntity>()
                .eq(FlowRegulationEntity::getId, id)
                .set(FlowRegulationEntity::getIsDeleted, 2)
        );

        if (update == 0) {
            log.error("数据库中未找到指定停用的科室");
            throw new HisException("未找到指定停用的科室");
        }

        return update;
    }

    /**
     * 启用指定科室
     * @param id
     * @return
     */
    @Override
    @Transactional
    public int enablePlace(Integer id) {

        // 1. 更新数据库数据
        int update = flowRegulationDao.update(null, new LambdaUpdateWrapper<FlowRegulationEntity>()
                .eq(FlowRegulationEntity::getId, id)
                .set(FlowRegulationEntity::getIsDeleted, 0)
        );

        // 2. 查询更新后的数据，缓存到 redis 中
        FlowRegulationEntity flowRegulationEntity = flowRegulationDao.selectById(id);

        if (update == 0) {
            log.error("科室 id：{} 启用失败", id);
            throw new HisException("启用失败，未找到待启用的科室或更新时出现问题，请联系管理员");
        }

        // 3. 往缓存中添加数据（可以不使用 lua，因为一个是存储数据，另一个是排队人数统计，刚启用时一般不会有，如果刚启用就有人排队，那就该 ZSET 就会存在，此时 ZADD 不执行）
        // 3.1 往 flow:regulation 中缓存科室调流数据
        redisTemplate.opsForHash().put(FlowRegulationConstants.FLOW_REGULATION, id.toString(), flowRegulationEntity);
        // 3.2 往 flow:place:rank 中缓存科室数据
        stringRedisTemplate.opsForZSet().addIfAbsent(FlowRegulationConstants.FLOW_PLACE_RANK, id.toString(), 0);

        log.debug("科室 id：{} 启用成功，成功往 redis 中缓存了数据：{}，{}", id, FlowRegulationConstants.FLOW_REGULATION, FlowRegulationConstants.FLOW_PLACE_RANK);

        return update;
    }


    /**
     * 删除或批量删除科室调流规则
     * @param ids    科室 id 数组
     * @return
     */
    @Override
    public int deleteFlowRegulation(Integer[] ids) {

        // 1. 逻辑删除（只有停用的科室才能删除）
        int update = flowRegulationDao.update(null, new LambdaUpdateWrapper<FlowRegulationEntity>()
                .in(FlowRegulationEntity::getId, Arrays.asList(ids))
                .eq(FlowRegulationEntity::getIsDeleted, 2)
                .set(FlowRegulationEntity::getIsDeleted, 1));

        if (update == 0) {
            log.error("数据库中未找到待删除的科室");
            throw new HisException("未找到待删除的科室");
        }

        log.debug("成功删除数据库中 {} 条科室数据", update);

        // 2. 删除缓存数据
        String[] fields = Arrays.stream(ids).map(String::valueOf).toArray(String[]::new);

        String lua = " local z = redis.call('ZREM', KEYS[1], unpack(ARGV)) " +                // 删除 flow:regulation:rank 中的科室
                     " local h = redis.call('HDEL', KEYS[2], unpack(ARGV)) " +                // 删除 flow:regulation:regulation 中的科室
                     " return {z, h} ";

        @SuppressWarnings("unchecked") // DefaultRedisScript List.class 没有指定泛型，但是安全，可以忽略警告
        List<Long> result = stringRedisTemplate.execute(
                new DefaultRedisScript<>(lua, List.class),
                Arrays.asList(
                        FlowRegulationConstants.FLOW_PLACE_RANK,
                        FlowRegulationConstants.FLOW_REGULATION
                ),
                (Object[]) fields
        );

        // 日志记录
        if (result != null && result.size() >= 2) {
            log.debug("在 ZSET {} 中删除了 {} 条数据, 在 Hash {} 中删除了 {} 条数据",
                    FlowRegulationConstants.FLOW_PLACE_RANK, result.get(0),
                    FlowRegulationConstants.FLOW_REGULATION, result.get(1));
        } else {
            log.warn("Lua 脚本执行返回异常结果: {}", result);
            throw new HisException("缓存数据删除失败，请联系管理员");
        }

        return update;
    }

    /**
     * 修改调流模式
     * @param value
     * @return
     */
    @Override
    public int changeFlowRegulationMode(String value) {
        return systemService.setSystemSettingItemValue("auto_flow_regulation", value);
    }
}
