package com.hmdp.utils;

import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final int COUNT_BITS = 32;
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){
        //生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond= now.toEpochSecond(ZoneOffset.UTC);
        long timestamp=nowSecond-BEGIN_TIMESTAMP;

        //生成序列号
        //1.获取当前日期，精确到天
        String date=now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2.自增长
        long count=stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);

        //拼接、返回
        //位运算
        return timestamp << COUNT_BITS | count;
    }
}
