package com.zorroe.cloud.job.admin.components;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 加锁（SET NX EX）
     */
    public boolean lock(String key, long timeout, TimeUnit unit) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "locked", timeout, unit);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放锁
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }
}