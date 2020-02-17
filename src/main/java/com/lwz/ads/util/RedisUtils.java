package com.lwz.ads.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author liweizhou 2020/2/2
 */
@Slf4j
@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public <T> T get(String key, Class<T> clz) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> T execute(Function<RedisTemplate<String, Object>, T> function) {
        return function.apply(redisTemplate);
    }

    /**
     * 获取分布式锁
     *
     * @param key          锁名
     * @param requestId    锁的密钥
     * @param expireSecond 锁的过期时间
     * @return
     */
    public boolean lock(String key, String requestId, int expireSecond) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, requestId, expireSecond, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("lock fail. key:{} err:{}", key, e.getMessage(), e);
            return false;
        }
    }

    private final String UNLOCK_LUA = "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end";

    /**
     * 释放分布式锁
     *
     * @param key       锁名
     * @param requestId 锁的密钥
     * @return
     */
    public boolean unlock(String key, String requestId) {
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript();
            redisScript.setScriptText(UNLOCK_LUA);
            redisScript.setResultType(Long.class);
            Long result = redisTemplate.execute(redisScript, Arrays.asList(key), requestId);
            return result.intValue() == 1;
        } catch (Exception e) {
            log.error("unlock fail. key:{} err:{}", key, e.getMessage(), e);
            return false;
        }
    }

}
