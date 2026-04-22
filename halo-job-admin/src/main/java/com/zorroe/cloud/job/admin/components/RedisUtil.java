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
     *
     * @param key 锁的key
     * @param expire 过期时间
     * @param unit 时间单位
     * @return 是否加锁成功
     */
    public boolean lock(String key, long expire, TimeUnit unit) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, "1", expire, unit)
        );
    }

    /**
     * 解锁
     *
     * @param key 锁的key
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * SET NX（不存在才设置）
     *
     * @param key key
     * @param value value
     * @param expire 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public Boolean setIfAbsent(String key, String value, long expire, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, expire, unit);
    }

    /**
     * 设置值
     *
     * @param key key
     * @param value value
     * @param expire 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long expire, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value.toString(), expire, unit);
    }

    /**
     * 获取值
     *
     * @param key key
     * @return value
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}