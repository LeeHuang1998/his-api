package com.leehuang.his.api.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehuang.his.api.common.constants.redis.FlowRegulationConstants;
import com.leehuang.his.api.db.dao.FlowRegulationDao;
import com.leehuang.his.api.db.entity.FlowRegulationEntity;
import com.leehuang.his.api.exception.HisException;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class FlowRegulationJobHandler {

    private final RedisTemplate<String, Object> redisTemplate;

    private final FlowRegulationDao flowRegulationDao;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每天早上 6 点营业前初始化调流系统
     */
    @XxlJob("initFlowRegulationSystem")
    @Transactional
    public void initFlowSystem() {

        // 1. 清空昨天的科室调流数据、各科室的排队人数 ZSET 以及各科室的排队人员姓名 ZSET
        stringRedisTemplate.delete(FlowRegulationConstants.FLOW_PLACE_RANK);
        redisTemplate.delete(FlowRegulationConstants.FLOW_REGULATION);

        // 1.1 使用 SCAN + UNLINK 删除掉所有科室排队人员姓名队列，若没有匹配的数据也会安全运行
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + "*")      // 匹配规则，以 flow:regulation:place_queue: 开头的所有 key
                    .count(100)                                                                    // 每次扫描的数量建议，不精确
                    .build();

            // 记录成功删除的键数量
            long deletedCount = 0;

            // 开始删除所有的 flow:regulation:place_queue:id
            // 根据配置的 options 执行 SCAN 命令并返回一个游标 Cursor
            // 执行时向 Redis 发送了第一条 SCAN 命令（SCAN 0（游标起始位置） MATCH flow:* COUNT 1000），将扫描到数据存放在 cursor 内部的缓冲区中
            // 例如当有 2580 个 key 匹配，第一次扫描到大概 1000 个 key（不精确，可能大可能小）并返回一个新的游标 ID，会将这些 key 缓存到内部缓冲区
            // 当 cursor.hasNext() 返回 true 时，表示缓冲区中还有数据，可以继续遍历，若 hasNext 为 false，就会根据之前的游标再执行一次 SCAN 命令，直到没有数据为止（返回的游标为 0）
            // 在 try 括号中写是为了自动关闭 cursor，上面 RedisCallback 的 execute 方法内部已经封装了 RedisConnection 的获取和释放逻辑，无需手动关闭
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {                                                          // cursor 对象的缓冲区中是否有数据
                    byte[] keyBytes = cursor.next();                                                // 取出当前遍历的 key（从缓冲区里拿出排在最前面的 Key）
                    connection.keyCommands().unlink(keyBytes);                                      // 调用 Redis 的 UNLINK 命令删除这个 Key
                    // DEL 是同步删除，如果 Key 包含大量数据（比如一个很大的 List 或 ZSet），会阻塞 Redis 主线程
                    // UNLINK 是异步删除，把 Key 从 keyspace 中移除，真正的内存释放会在后台线程中进行，因此不会阻塞 Redis

                    deletedCount++;     // 计数
                }

                log.info("成功批量删除 {} 个匹配 {} 的Key", deletedCount, FlowRegulationConstants.FLOW_REGULATION_QUEUE_PREFIX + "*");
            } catch (Exception e) {
                // 按需记录日志
                log.error("批量删除 flow:regulation:queue:* 失败", e);
                throw new HisException("批量删除 flow:regulation:queue:* 失败", e);
            }
            // 回调类型是 RedisCallback<Void>，Void 不需要返回值，所以返回 null
            return null;
        });

        // 2. 查询所有科室调流数据（已启用的科室）
        List<FlowRegulationEntity> list = flowRegulationDao.selectList(new LambdaQueryWrapper<FlowRegulationEntity>()
                .eq(FlowRegulationEntity::getIsDeleted, 0));

        // 3. 转换为 Map<place, entity>
        Map<String, FlowRegulationEntity> placeMap = list.stream()
                .filter(entity -> entity.getPlace() != null && !entity.getPlace().trim().isEmpty())
                .collect(Collectors.toMap(
                        entity -> entity.getId().toString(),
                        Function.identity(),
                        (existing, replacement) -> existing         // 若出现重复的 key，则保留第一个（existing：流中先出现的元素对应的 value，replacement：流中后出现的元素对应的 value）
                ));

        // 4. 初始化 Redis
        // 4.1 缓存科室调流数据
        if (!placeMap.isEmpty()) {
            // 将科室调流数据存入 Redis（regulationMap)
            redisTemplate.opsForHash().putAll(FlowRegulationConstants.FLOW_REGULATION, placeMap);
            // 设置过期时间（例如 25 小时）
            redisTemplate.expire(FlowRegulationConstants.FLOW_REGULATION, 25, TimeUnit.HOURS);
            log.info("科室调流数据缓存刷新成功，共 {} 条记录", placeMap.size());
        }

        // 4.2 排队人数 ZSET 初始化
        list.forEach(place -> {
            // candidates 缓存
            stringRedisTemplate.opsForZSet().add(FlowRegulationConstants.FLOW_PLACE_RANK, place.getId().toString(), 0);
        });

        // 5. 初始化成功标记
        stringRedisTemplate.opsForValue().set(FlowRegulationConstants.FLOW_INIT_STATUS, "OK", 1, TimeUnit.DAYS);

        log.info("【调流系统】初始化完成");
    }
}
