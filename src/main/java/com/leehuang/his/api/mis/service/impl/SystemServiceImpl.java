package com.leehuang.his.api.mis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.leehuang.his.api.db.dao.SystemDao;
import com.leehuang.his.api.db.entity.SystemEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.service.SystemService;
import com.leehuang.his.api.mq.message.CacheDeleteRetryMessage;
import com.leehuang.his.api.mq.producer.CacheDeleteRetryDelayProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service("SystemService")
@RequiredArgsConstructor
public class SystemServiceImpl extends ServiceImpl<SystemDao, SystemEntity> implements SystemService {

    private final StringRedisTemplate redisTemplate;

    private final SystemDao systemDao;

    private final CacheDeleteRetryDelayProducer delayProducer;

    private static final String CACHE_KEY_PREFIX = "setting:";

    // 防御多次重复请求一个不存在的 item 缓存中无数据时，查询数据库，避免数据库被压垮
    private static final String NULL_VALUE = "__NULL__";

    /**
     * 获取系统设置项的值
     * @param item  系统设置项
     * @return
     */
    @Override
    public String getSystemSettingItemValue(String item) {

        // 1. 查询缓存中数据
        String cacheKey = CACHE_KEY_PREFIX + item;
        String value = redisTemplate.opsForValue().get(cacheKey);

        // 查询到数据，返回数据
        if (value != null && !value.isBlank()) {
            if (NULL_VALUE.equals(value)) {
                log.error("该 item 不存在，item: {}", item);
                throw new HisException("未找到该设置项，请勿重复查询，并联系管理员");
            }
            return value;
        }

        // 2. 缓存未命中，查询数据库
        log.warn("缓存未命中，查询数据库，item: {}", item);
        SystemEntity entity = systemDao.selectOne(
                new LambdaQueryWrapper<SystemEntity>().eq(SystemEntity::getItem, item)
        );

        // 3. 数据库中存在数据，写入缓存并返回数据
        if (entity != null && StringUtils.hasText(entity.getValue())) {
            redisTemplate.opsForValue().set(cacheKey, entity.getValue());
            log.debug("设置项【{}】已写入缓存，值为 【{}】", item, entity.getValue());
            return entity.getValue();
        } else {
            // 数据库中也不存在，在缓存中标记为 __NULL__，若下次再有该不存在于数据库中的 item 则会在上面直接抛异常，避免多次查询数据库
            redisTemplate.opsForValue().set(cacheKey, NULL_VALUE, 3, TimeUnit.HOURS);
            log.error("未找到设置项：{}", item);
            throw new HisException("未找到该设置项，请联系管理员");
        }
    }

    /**
     * 设置系统设置项的值
     * @param item  系统设置项
     * @param value 系统设置项的值
     * @return
     */
    @Override
    @Transactional
    public Integer setSystemSettingItemValue(String item, String value) {
        // 1. 修改数据库中数据
        int update = systemDao.update(null, new LambdaUpdateWrapper<SystemEntity>()
                .eq(SystemEntity::getItem, item)
                .set(SystemEntity::getValue, value)
        );

        if (update == 0) {
            log.error("未找到设置项：{}，修改值为：{}", item, value);
            throw new HisException("未找到该设置项");
        }

        // 2. 删除缓存中数据值，下次读取时缓存
        String cacheKey = CACHE_KEY_PREFIX + item;
        try {
            redisTemplate.delete(cacheKey);
            log.debug("设置项 [{}] 已更新，缓存已删除", item);
        } catch (Exception e) {
            log.error("删除缓存失败，准备发送重试消息。item: {}", item, e);
            try {
                CacheDeleteRetryMessage message = new CacheDeleteRetryMessage();
                message.setCacheKey(cacheKey);
                message.setRetryCount(0);
                delayProducer.sendCacheDeleteRetryDelayMessage(message, 10000);
            } catch (Exception mqEx) {
                // MQ 也挂了：记录严重错误日志，但不回滚数据库事务，留给人工介入或定时任务修复
                log.error("【严重错误】发送缓存重试消息失败，可能导致数据不一致。item: {}", item, mqEx);
            }
        }
        return update;
    }
}
