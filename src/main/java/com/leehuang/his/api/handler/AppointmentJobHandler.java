package com.leehuang.his.api.handler;

import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentJobHandler {

    private final StringRedisTemplate redisTemplate;

    /**
     * 项目启动后，创建了明天起 30 天后的缓存数据，若有日期缓存，需要添加该日期第 32 天后的缓存数据（每天 23 点执行，所以是 32 天后的数据）
     * （从明天开始，明天的三十天后，就是今天的 31 天后，若明天过期，则需要添加后天的 30 天后的数据，即今天的 32 天后）
     */
    @XxlJob("cacheAppointmentDataJobHandler")
    public void cacheAppointmentData() {
        // 默认数据
        String value = redisTemplate.opsForValue().get("setting#appointment_number");
        int maxNum = value != null ? Integer.parseInt(value) : 200;
        int realNum = 0;

        LocalDate cacheDate = LocalDate.now().plusDays(32);
        String key = "appointment#" + cacheDate;

        // 缓存数据
        redisTemplate.opsForHash().putAll(key, new HashMap<String, Object>() {{
            put("maxNum", String.valueOf(maxNum));
            put("realNum", String.valueOf(realNum));
        }});

        LocalDateTime expireTime = cacheDate.plusDays(1).atStartOfDay();
        Date expireDate = Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant());
        redisTemplate.expireAt(key, expireDate);
        log.info("生成了 {} 的体检日程缓存", cacheDate);
    }
}
